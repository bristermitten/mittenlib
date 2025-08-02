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
	api(libs.jspecify)


	testImplementation(libs.jimfs)
	testImplementation(libs.mockbukkit)

	// Property-based testing with jqwik
	testImplementation("net.jqwik:jqwik:1.9.3")
	testRuntimeOnly("net.jqwik:jqwik-engine:1.9.3")
}

tasks.compileTestJava {
	sourceCompatibility = JavaVersion.VERSION_21.toString()
	targetCompatibility = JavaVersion.VERSION_21.toString()
}

tasks.test {
	useJUnitPlatform {
		includeEngines("jqwik")
	}
}
