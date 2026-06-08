plugins {
    id("mittenlib.java-conventions")
    id("me.champeau.jmh") version "0.7.3"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = sourceCompatibility
}

jmh {
    warmupIterations.set(5)
    iterations.set(5)
    failOnError.set(true)
    fork.set(1)
    resultFormat.set("JSON")
}

tasks.javadoc {
    // This module doesn't need to be documented so disable the annoying warnings
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
}

tasks.processJmhResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

dependencies {
    annotationProcessor(project(":annotation-processor"))
    implementation(project(":core"))

    implementation(libs.jackson.databind)
    implementation(libs.gson)
    implementation(libs.snakeyaml)
}
