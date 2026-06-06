import net.ltgt.gradle.errorprone.errorprone

java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = sourceCompatibility
}

dependencies {
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
		"RedundantSuppression"
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

tasks.withType<JavaCompile>().configureEach {
	options.errorprone.excludedPaths.set(".*/build/generated/.*")
}