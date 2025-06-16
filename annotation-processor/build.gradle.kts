java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = sourceCompatibility
}

dependencies {
	implementation(project(":core"))
	implementation(libs.javapoet)
	implementation(libs.bundles.autoservice)
	@Suppress(
		"GradlePackageUpdate",
		"RedundantSuppression"
	) // This is deliberately kept low, so it syncs with the spigot gson version
	implementation(libs.gson)

	implementation(libs.guice)

	implementation(libs.jetbrains.annotations)
	annotationProcessor(libs.auto.service)

	testImplementation(libs.mockito.core)
	testImplementation(libs.compile.testing)
}
