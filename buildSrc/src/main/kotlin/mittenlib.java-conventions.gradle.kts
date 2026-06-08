import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    `java-library`
    id("net.ltgt.errorprone")
    id("com.github.spotbugs")
}

val libs = versionCatalogs.named("libs")

group = "me.bristermitten"
version = "6.0.0-SNAPSHOT"

spotbugs {
    effort = Effort.MORE
    excludeFilter.set(rootProject.file("gradle/spotbugs-exclude.xml"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = sourceCompatibility
    withSourcesJar()
    withJavadocJar()

    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    compileOnly(libs.findLibrary("spigot-api").get())

    testImplementation(libs.findLibrary("spigot-api").get())
    testImplementation(libs.findLibrary("junit-api").get())
    testImplementation(libs.findLibrary("mockito-inline").get())
    testImplementation(libs.findLibrary("assertj-core").get())
    testRuntimeOnly(libs.findLibrary("junit-engine").get())
    testRuntimeOnly(libs.findLibrary("junit-launcher").get())

    errorprone(libs.findLibrary("errorprone").get())
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

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.isFork = true

    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        warn("UnnecessarilyFullyQualified")
    }
    options.isIncremental = true
}

tasks.withType<Javadoc>().configureEach {
    isFailOnError = false
    val options = options as StandardJavadocDocletOptions
    options.tags("apiNote:a:API Note:")

    if (JavaVersion.current().isJava9Compatible) {
        options.addBooleanOption("html5", true)
    }
    options.links("https://helpch.at/docs/1.8.8/")
    options.links("https://javadoc.io/doc/net.kyori/adventure-api/latest/")
    options.links("https://google.github.io/guice/api-docs/latest/javadoc/")
}

tasks.withType<SpotBugsTask>().configureEach {
    reports {
        create("html") {
            required.set(true)
            outputLocation.set(layout.buildDirectory.file("reports/spotbugs/spotbugs.html"))
        }
    }
}
