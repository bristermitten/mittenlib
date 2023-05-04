plugins {
    kotlin("jvm") version "1.8.21"
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":core"))
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.squareup:javapoet:1.13.0")
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")

    @Suppress(
        "GradlePackageUpdate",
        "RedundantSuppression"
    ) // This is deliberately kept low in order to sync with the spigot gson version
    implementation("com.google.code.gson:gson:2.3.1")

    implementation("com.google.inject:guice:5.1.0")

    implementation("org.jetbrains:annotations:23.0.0")
    annotationProcessor("com.google.auto.service:auto-service:1.0.1")

    testImplementation("org.mockito:mockito-core:4.8.0")
    testImplementation("com.google.testing.compile:compile-testing:0.19")
}
