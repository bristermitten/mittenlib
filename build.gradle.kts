plugins {
    java
    `java-library`
    `maven-publish`
    id("io.freefair.aggregate-javadoc") version "6.3.0"

}

subprojects {
    apply<JavaPlugin>()
    apply<JavaLibraryPlugin>()
    apply<MavenPublishPlugin>()

    val libs = rootProject.libs
    group = "me.bristermitten"
    version = "4.2.3-SNAPSHOT"

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
        testRuntimeOnly(libs.junit.engine)
    }

    tasks.test {
        useJUnitPlatform()
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        setForkEvery(100)
        reports.html.required.set(false)
        reports.junitXml.required.set(false)
    }

    tasks.javadoc {
        val options = options as StandardJavadocDocletOptions
        if (JavaVersion.current().isJava9Compatible) {
            options.addBooleanOption("html5", true)
        }
        options.links("https://helpch.at/docs/1.8.8/")
        options.links("https://javadoc.io/doc/net.kyori/adventure-api/latest/")
        options.links("https://google.github.io/guice/api-docs/latest/javadoc/")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.isFork = true
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
                    artifactId = "mittenlib-$artifactId"
                }
            }
        }
    }
}
