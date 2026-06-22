plugins {
    java
    scala
    id("io.github.cosmicsilence.scalafix")
}

scala {
    scalaVersion = "3.3.6"
}

tasks.withType<ScalaCompile> {
    scalaCompileOptions.additionalParameters = listOf("-Wunused:all")
}
