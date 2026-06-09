plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.spotbugs.gradle)
    implementation(libs.errorprone.gradle)
    implementation(libs.spotless.gradle)
    implementation(libs.githook.gradle)
}
