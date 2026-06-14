plugins {
    id("mittenlib.java-conventions")
    id("mittenlib.publishing-conventions")
    scala
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = sourceCompatibility
}

scala {
    scalaVersion = "3.3.6"
}

dependencies {
    implementation(libs.javapoet)
    implementation(project(":core"))
    implementation(libs.guice)
}
