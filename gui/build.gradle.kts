repositories {
	maven("https://repo.aikar.co/content/groups/aikar/")
}

dependencies {
	implementation(project(":core"))
	implementation(project(":record-generator:api"))
	testAnnotationProcessor(project(":record-generator:processor"))

	testImplementation("com.github.stefanbirkner:system-lambda:1.2.1")
}
