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
}

