import net.ltgt.gradle.errorprone.ErrorPronePlugin
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
	java
	`java-library`
	`maven-publish`
	id("io.freefair.aggregate-javadoc") version "6.3.0"
	id("net.ltgt.errorprone") version "+"
}


fun Javadoc.configureJavadoc() {
	val options = options as StandardJavadocDocletOptions
	options.tags("apiNote:a:API Note:")

	if (JavaVersion.current().isJava9Compatible) {
		options.addBooleanOption("html5", true)
	}
	options.tags("apiNote:a:API Note:")
	options.links("https://helpch.at/docs/1.8.8/")
	options.links("https://javadoc.io/doc/net.kyori/adventure-api/latest/")
	options.links("https://google.github.io/guice/api-docs/latest/javadoc/")
}

tasks.aggregateJavadoc {
	configureJavadoc()
}

subprojects {
	apply<JavaPlugin>()
	apply<JavaLibraryPlugin>()
	apply<MavenPublishPlugin>()
	apply<ErrorPronePlugin>()

	val libs = rootProject.libs
	group = "me.bristermitten"
	version = "4.5.0-SNAPSHOT"

	java {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = sourceCompatibility
		withSourcesJar()
		withJavadocJar()

		toolchain {
			languageVersion = JavaLanguageVersion.of(21)
		}
	}

	repositories {
		mavenCentral()
		maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
		maven("https://oss.sonatype.org/content/repositories/snapshots")
		maven("https://oss.sonatype.org/content/repositories/central")
	}

	dependencies {
		compileOnly(libs.spigot.api)

		testImplementation(libs.spigot.api)
		testImplementation(libs.junit.api)
		testImplementation(libs.mockito.core)
		testImplementation(libs.assertj.core)
		testRuntimeOnly(libs.junit.engine)
		testRuntimeOnly(libs.junit.launcher)

		errorprone("com.google.errorprone:error_prone_core:+")
	}

	tasks.test {
		useJUnitPlatform()
		maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
		reports.html.required.set(false)
		reports.junitXml.required.set(false)

		testLogging {
			showExceptions = true
			showStandardStreams = true
			events = setOf(
				TestLogEvent.FAILED,
				TestLogEvent.PASSED,
				TestLogEvent.SKIPPED
			)
			exceptionFormat = TestExceptionFormat.FULL
		}
	}


	tasks.withType<JavaCompile> {
		options.encoding = "UTF-8"
		options.isFork = true

		options.errorprone.disableWarningsInGeneratedCode.set(true)
	}
	tasks.javadoc {
		configureJavadoc()
	}

	publishing {
		repositories {
			maven {
				val releasesRepoUrl = "https://repo.bristermitten.me/releases"
				val snapshotsRepoUrl = "https://repo.bristermitten.me/snapshots"
				url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

				credentials {
					username = project.findProperty("mavenUser")?.toString() ?: System.getenv("MAVEN_USER")
					password = project.findProperty("mavenPassword")?.toString() ?: System.getenv("MAVEN_PASSWORD")
				}
			}

			publications {
				create<MavenPublication>("maven") {
					from(components["java"])
					artifactId = "mittenlib${project.path.replace(":", "-")}"
				}
			}
		}
	}
}
