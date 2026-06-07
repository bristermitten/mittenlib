plugins {
    id("mittenlib.java-conventions")
    id("mittenlib.publishing-conventions")
}

dependencies {
    implementation(project(":core"))
    implementation(libs.adventure.text.minimessage)
}
