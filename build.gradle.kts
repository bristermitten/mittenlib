plugins {
    alias(libs.plugins.freefair.aggregate.javadoc)
}

dependencies {
    subprojects.forEach { subproject ->
        subproject.plugins.withId("java") {
            javadoc(subproject)
        }
    }

    // Workaround from https://github.com/freefair/gradle-plugins/issues/1522
    javadocClasspath(libs.spigot.api)
    javadocClasspath(libs.placeholderapi)
}

tasks.javadoc {
    isFailOnError = false
    val options = options as StandardJavadocDocletOptions
    options.tags("apiNote:a:API Note:")

    if (JavaVersion.current().isJava9Compatible) {
        options.addBooleanOption("html5", true)
    }
    options.links("https://helpch.at/docs/1.8.8/")
    options.links("https://javadoc.io/doc/net.kyori/adventure-api/latest/")
    options.links("https://google.github.io/guice/api-docs/latest/javadoc/")
}



tasks.register<Copy>("copyJavadocsToDocs") {
    description = "Copy all the generated Javadocs to the Docusaurus static folder"
    dependsOn("javadoc")
    from(layout.buildDirectory.dir("docs/javadoc"))
    into(file("docs-site/static/javadoc"))
}