java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = sourceCompatibility
}


dependencies {
	implementation(project(":core"))
	implementation(project(":record-generator:api"))
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
