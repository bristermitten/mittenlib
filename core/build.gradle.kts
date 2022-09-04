plugins {
    `java-library`
}

dependencies {
    api("com.google.inject:guice:5.1.0")
    api("net.kyori:adventure-api:4.11.0")
    api("net.kyori:adventure-platform-bukkit:4.1.2")
    testImplementation("com.google.jimfs:jimfs:1.2")
}


tasks.compileTestJava {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
}
