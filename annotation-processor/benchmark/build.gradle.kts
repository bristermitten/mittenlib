plugins {
    id("me.champeau.jmh") version "0.6.6"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = sourceCompatibility
}

jmh {
    warmupIterations.set(1)
    iterations.set(2)
    fork.set(2)
}

tasks.processJmhResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}


dependencies {
    annotationProcessor(project(":annotation-processor"))
    implementation(project(":core"))
}
