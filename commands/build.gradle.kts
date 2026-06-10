plugins {
    id("mittenlib.java-conventions")
    id("mittenlib.publishing-conventions")
}

dependencies {
    implementation(project(":core"))
    api(libs.acf.paper)
}
