rootProject.name = "mittenlib"
include("annotation-processor")
include("core")
include("demo")
include("commands")
include("gui")
include("minimessage")
include("papi")
include("annotation-processor:benchmark")
findProject(":annotation-processor:benchmark")?.name = "benchmark"


include("record-generator")
include("record-generator:api")
include("record-generator:processor")
include("record-generator:integration-test")

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
			name = "Spigot Snapshots"
		}
		maven("https://repo.papermc.io/repository/maven-public/") {
			name = "PaperMC"
		}
		maven("https://oss.sonatype.org/content/repositories/snapshots") {
			name = "Sonatype Snapshots"
		}
		maven("https://oss.sonatype.org/content/repositories/central/") {
			name = "Sonatype Central"
		}
		maven("https://repo.aikar.co/content/groups/aikar/") {
			name = "Aikar"
		}
		maven("https://repo.extendedclip.com/releases/") {
			name = "ExtendedClip"
		}
		maven("https://repo.glaremasters.me/repository/public/") {
			name = "GlareMasters"
		}
		maven("https://jitpack.io") {
			name = "JitPack"
			content {
				includeGroupAndSubgroups("com.github")
			}

		}
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
			library("junit-launcher", "org.junit.platform", "junit-platform-launcher").withoutVersion()

			version("assertj", "3.27.3")
			library("assertj-core", "org.assertj", "assertj-core").versionRef("assertj")


			version("mockito", "4.8.0")
			library("mockito-core", "org.mockito", "mockito-core").versionRef("mockito")
			library("mockito-inline", "org.mockito", "mockito-inline").versionRef("mockito")

			library(
				"mockbukkit",
				"com.github.bristermitten",
				"MockBukkit"
			).version("93122b01fcbb3f66b211aede5eb66000e78b117f")

			// Other Libraries
			library("javapoet", "com.squareup", "javapoet").version("1.13.0")
			library("jetbrains-annotations", "org.jetbrains", "annotations").version("23.0.0")
			//jspecify
			library("jspecify", "org.jspecify", "jspecify").version("1.0.0")
			library("acf-paper", "co.aikar", "acf-paper").version("0.5.1-SNAPSHOT")
			library("placeholderapi", "me.clip", "placeholderapi").version("2.11.6")
			version("aptk", "0.22.5")
			library("aptk-tools", "io.toolisticon.aptk", "aptk-tools").versionRef("aptk")
			library("aptk-compilermessages-api", "io.toolisticon.aptk", "aptk-compilermessages-api").versionRef("aptk")
			library(
				"aptk-compilermessages-processor",
				"io.toolisticon.aptk",
				"aptk-compilermessages-processor"
			).versionRef("aptk")
			library(
				"aptk-annotationwrapper-processor",
				"io.toolisticon.aptk",
				"aptk-annotationwrapper-processor"
			).versionRef("aptk")
			library(
				"aptk-annotationwrapper-api",
				"io.toolisticon.aptk",
				"aptk-annotationwrapper-api"
			).versionRef("aptk")


            library("cute", "io.toolisticon.cute", "cute").version("1.9.0")
			library("chalk", "com.github.tomas-langer", "chalk").version("1.0.2")
		}
	}
}
