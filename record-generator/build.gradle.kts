// don't publish this root module as it'll be empty anyway
tasks.withType<AbstractPublishToMaven>().configureEach {
	enabled = false
}
