java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = sourceCompatibility
}


dependencies {
	implementation(project(":core"))
	implementation(project(":record-generator:api"))

	testImplementation(libs.cute)
	testImplementation(libs.mockito.core)
	testImplementation(libs.compile.testing)
	testAnnotationProcessor(project(":record-generator:processor"))
}
