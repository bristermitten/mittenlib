repositories {
	maven("https://repo.aikar.co/content/groups/aikar/")
}

dependencies {
	implementation(project(":core"))
	api(libs.acf.paper)
}
