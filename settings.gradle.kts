rootProject.name = "mittenlib"
include("annotation-processor")
include("core")
include("commands")
include("minimessage")
include("papi")
include("annotation-processor:benchmark")
findProject(":annotation-processor:benchmark")?.name = "benchmark"

dependencyResolutionManagement {
	repositories {
		mavenCentral()
		maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
		maven("https://oss.sonatype.org/content/repositories/snapshots")
		maven("https://oss.sonatype.org/content/repositories/central")
		maven("https://repo.papermc.io/repository/maven-public")
		maven("https://repo.aikar.co/content/groups/aikar/")
		maven("https://repo.extendedclip.com/releases/")
	}
	versionCatalogs {
		create("libs") {
			// Spigot
			version("spigot", "1.8.8-R0.1-SNAPSHOT")
			library("spigot-api", "org.spigotmc", "spigot-api").versionRef("spigot")

			// Google Libraries
			version("guice", "6.0.0")
			library("guice", "com.google.inject", "guice").versionRef("guice")

			version("auto-service", "1.0.1")
			library(
				"auto-service-annotations", "com.google.auto.service", "auto-service-annotations"
			).versionRef("auto-service")
			library("auto-service", "com.google.auto.service", "auto-service").versionRef("auto-service")
			bundle("autoservice", listOf("auto-service-annotations", "auto-service"))

			library("jimfs", "com.google.jimfs", "jimfs").version("1.2")
			library("gson", "com.google.code.gson", "gson").version("2.3.1")
			library("compile-testing", "com.google.testing.compile", "compile-testing").version("0.19")

			// Kyori Adventure
			version("adventure-api", "4.11.0")
			library("adventure-api", "net.kyori", "adventure-api").versionRef("adventure-api")

			version("adventure-platform", "4.1.2")
			library(
				"adventure-platform-bukkit", "net.kyori", "adventure-platform-bukkit"
			).versionRef("adventure-platform")

			library("adventure-text-minimessage", "net.kyori", "adventure-text-minimessage").version("4.10.1")

			// Testing
			version("junit", "5.8.1")
			library("junit-api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
			library("junit-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")

			version("mockito", "4.8.0")
			library("mockito-core", "org.mockito", "mockito-core").versionRef("mockito")

			library("mockbukkit", "org.mockbukkit.mockbukkit", "mockbukkit-v1.21").version("4.0.0")

			// Other Libraries
			library("javapoet", "com.squareup", "javapoet").version("1.13.0")
			library("jetbrains-annotations", "org.jetbrains", "annotations").version("23.0.0")
			library("acf-paper", "co.aikar", "acf-paper").version("0.5.1-SNAPSHOT")
			library("placeholderapi", "me.clip", "placeholderapi").version("2.11.6")
		}
	}
}
