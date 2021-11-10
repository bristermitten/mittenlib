java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = sourceCompatibility
}

dependencies {
    implementation(project(":core"))
    implementation("com.squareup:javapoet:1.13.0")
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")
    annotationProcessor("com.google.auto.service:auto-service:1.0.1")

    testImplementation("org.mockito:mockito-core:4.0.0")
    testImplementation("com.google.testing.compile:compile-testing:0.19")
}
