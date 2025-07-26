dependencies {
	implementation(project(":core"))
	implementation(project(":record-generator:api"))
	testAnnotationProcessor(project(":record-generator:processor"))

	testImplementation("com.github.stefanbirkner:system-lambda:1.2.1")
	testImplementation(libs.mockbukkit)
}


tasks.compileTestJava {
	sourceCompatibility = JavaVersion.VERSION_21.toString()
	targetCompatibility = JavaVersion.VERSION_21.toString()
}