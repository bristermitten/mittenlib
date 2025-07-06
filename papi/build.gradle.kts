repositories {
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    implementation(project(":core"))
    compileOnly(libs.placeholderapi)
}
