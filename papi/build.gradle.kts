repositories {
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    implementation(project(":core"))
    compileOnly("me.clip:placeholderapi:2.11.6")
}
