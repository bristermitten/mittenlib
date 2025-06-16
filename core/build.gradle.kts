plugins {
	`java-library`
}
repositories {
	maven("https://repo.papermc.io/repository/maven-public")
}

dependencies {
	api(libs.guice)
	api(libs.adventure.api)
	api(libs.adventure.platform.bukkit)
	testImplementation(libs.jimfs)
	testImplementation(libs.mockbukkit)
}


tasks.compileTestJava {
	sourceCompatibility = JavaVersion.VERSION_21.toString()
	targetCompatibility = JavaVersion.VERSION_21.toString()
}
