import xyz.jpenilla.runpaper.task.RunServer

plugins {
	id("xyz.jpenilla.run-paper") version "2.3.1"
	id("com.gradleup.shadow") version "8.3.8"

}

dependencies {
	implementation(project(":core"))
	implementation(project(":record-generator:api"))
	implementation(project(":gui"))
	annotationProcessor(project(":record-generator:processor"))

	testImplementation("com.github.stefanbirkner:system-lambda:1.2.1")
	testImplementation(libs.mockbukkit)
}


tasks.compileTestJava {
	sourceCompatibility = JavaVersion.VERSION_21.toString()
	targetCompatibility = JavaVersion.VERSION_21.toString()
}

tasks.shadowJar {
	relocate("com.google.common", "me.bristermitten.mittenlib.demo.shaded.com.google.common")
}

tasks.withType<RunServer> {
	javaLauncher = javaToolchains.launcherFor {
		vendor = JvmVendorSpec.JETBRAINS
		languageVersion = JavaLanguageVersion.of(17)
	}
	jvmArgs("-XX:+AllowEnhancedClassRedefinition")

	minecraftVersion("1.8.8")
	downloadPlugins {
		url("https://ci.viaversion.com/job/ViaVersion/1180/artifact/build/libs/ViaVersion-5.4.1.jar")
		url("https://www.spigotmc.org/resources/vault.34315/download?version=344916")
		url("https://github.com/EssentialsX/Essentials/releases/download/2.21.1/EssentialsX-2.21.1.jar")
	}
}