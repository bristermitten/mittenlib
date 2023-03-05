repositories {
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    implementation(project(":core"))
    compileOnly("me.clip:placeholderapi:2.11.2")
}
