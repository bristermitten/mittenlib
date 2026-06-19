plugins {
    id("mittenlib.java-conventions")
    id("mittenlib.publishing-conventions")
}

dependencies {
    api(project(":core"))
    api(libs.mockito.core)
    api(libs.mockbukkit)
}
