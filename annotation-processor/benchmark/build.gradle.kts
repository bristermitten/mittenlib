plugins {
	id("me.champeau.jmh") version "0.7.3"
}

java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = sourceCompatibility
}

jmh {
	warmupIterations.set(2)
	iterations.set(3)
	failOnError.set(true)
	fork.set(2)
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

	implementation("com.fasterxml.jackson.core:jackson-core:2.19.0")
	implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
}

// don't publish as not public api
tasks.withType<AbstractPublishToMaven>().configureEach {
	enabled = false
}
