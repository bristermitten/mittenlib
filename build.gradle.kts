plugins {
    java
    `java-library`
    `maven-publish`
    id("io.freefair.aggregate-javadoc") version "6.3.0"
}

subprojects {
    apply<JavaPlugin>()
    apply<JavaLibraryPlugin>()
    apply<MavenPublishPlugin>()


    group = "me.bristermitten"
    version = "3.2.2-SNAPSHOT"

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = sourceCompatibility
        withSourcesJar()
        withJavadocJar()
    }

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://oss.sonatype.org/content/repositories/central")
    }

    dependencies {
        compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")

        testImplementation("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
        testImplementation("org.mockito:mockito-core:3.+")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.javadoc {
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        }

    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    publishing {
        repositories {
            maven {
                val releasesRepoUrl = "https://repo.bristermitten.me/repository/maven-releases"
                val snapshotsRepoUrl = "https://repo.bristermitten.me/repository/maven-snapshots"
                url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

                credentials {
                    username = project.findProperty("mavenUser")?.toString() ?: System.getenv("MAVEN_USER")
                    password = project.findProperty("mavenPassword")?.toString() ?: System.getenv("MAVEN_PASSWORD")
                }
            }

            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                    artifactId = "mittenlib-$artifactId"
                }
            }
        }
    }
}


