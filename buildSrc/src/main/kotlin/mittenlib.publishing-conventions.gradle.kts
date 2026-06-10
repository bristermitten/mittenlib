plugins {
    `maven-publish`
}

publishing {
    repositories {
        maven {
            val releasesRepoUrl = "https://repo.bristermitten.me/releases"
            val snapshotsRepoUrl = "https://repo.bristermitten.me/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

            credentials {
                username = project.findProperty("mavenUser")?.toString() ?: System.getenv("MAVEN_USER")
                password = project.findProperty("mavenPassword")?.toString() ?: System.getenv("MAVEN_PASSWORD")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "mittenlib${project.path.replace(":", "-")}"
        }
    }
}
