import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("mittenlib.java-conventions")
    id("mittenlib.publishing-conventions")
    scala
}

scala {
    scalaVersion = "3.3.6"
}

val generateProcessorServiceFile by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/resources")
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("META-INF/services/javax.annotation.processing.Processor").asFile
        file.parentFile.mkdirs()
        file.writeText("me.bristermitten.mittenlib.codegen.MittenLibCodegenProcessor\n")
    }
}

sourceSets {
    main {
        scala {
            srcDirs("src/main/scala", "src/main/java")
        }
        resources {
            srcDir(generateProcessorServiceFile)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = sourceCompatibility
}

tasks.compileJava {
    enabled = true
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.excludedPaths.set(".*/build/generated/.*")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":record-generator:api"))
    implementation(project(":codegen-dsl"))
    implementation(libs.javapoet)
    implementation(libs.aptk.tools)
    implementation(libs.aptk.compilermessages.api)
    implementation(libs.aptk.annotationwrapper.api)
    annotationProcessor(libs.aptk.compilermessages.processor)
    annotationProcessor(libs.aptk.annotationwrapper.processor)
    implementation(libs.bundles.autoservice)
    implementation(libs.chalk)

    implementation(libs.guice)

    implementation(libs.jetbrains.annotations)
    annotationProcessor(libs.auto.service)

    testImplementation(libs.cute)
    testImplementation(libs.mockito.core)
    testImplementation(libs.compile.testing)
    testAnnotationProcessor(project(":annotation-processor"))
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
