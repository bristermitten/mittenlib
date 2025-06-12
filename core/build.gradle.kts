plugins {
	`java-library`
}
repositories {
	maven("https://repo.papermc.io/repository/maven-public")
}

dependencies {
	api("com.google.inject:guice:6.0.0")
	api("net.kyori:adventure-api:4.11.0")
	api("net.kyori:adventure-platform-bukkit:4.1.2")
	testImplementation("com.google.jimfs:jimfs:1.2")
	testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.0.0")
}


tasks.compileTestJava {
	sourceCompatibility = JavaVersion.VERSION_21.toString()
	targetCompatibility = JavaVersion.VERSION_21.toString()
}
