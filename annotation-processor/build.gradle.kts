import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("mittenlib.java-conventions")
    id("mittenlib.publishing-conventions")
    scala
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = sourceCompatibility
}

scala {
    scalaVersion = "3.3.6"
}

dependencies {
    implementation(project(":codegen-dsl"))
    implementation(project(":core"))
    implementation(libs.javapoet)
    implementation(libs.aptk.tools)
    implementation(libs.aptk.compilermessages.api)
    implementation(libs.aptk.annotationwrapper.api)
    annotationProcessor(libs.aptk.compilermessages.processor)
    annotationProcessor(libs.aptk.annotationwrapper.processor)
    implementation(libs.bundles.autoservice)
    implementation(libs.chalk)
    @Suppress(
        "GradlePackageUpdate",
        "RedundantSuppression",
    ) // This is deliberately kept low, so it syncs with the spigot gson version
    implementation(libs.gson)

    implementation(libs.guice)

    implementation(libs.jspecify)
    implementation(libs.jetbrains.annotations)
    annotationProcessor(libs.auto.service)

    testImplementation(libs.cute)
    testImplementation(libs.mockito.core)
    testImplementation(libs.compile.testing)

    // we use these to test our compatibility with the actual annotations
    implementation("jakarta.validation:jakarta.validation-api:3.1.1")
    implementation("javax.validation:validation-api:2.0.1.Final")
    testAnnotationProcessor(project(":annotation-processor"))
}

sourceSets {
    main {
        scala {
            srcDirs("src/main/scala", "src/main/java")
        }
    }
}

tasks.compileJava {
    enabled = false
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.excludedPaths.set(".*/build/generated/.*")
}

tasks.jacocoTestReport {
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/ConfigClassParserCompilerMessages*",
                        "**/CustomDeserializersCompilerMessages*",
                        "**/CustomDeserializerForWrapper*",
                    )
                }
            },
        ),
    )
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
