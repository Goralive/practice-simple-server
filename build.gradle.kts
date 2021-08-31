plugins {
    java
}

group = "practice-simple-server"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

object Version {
    const val JUNIT = "5.7.0"
    const val SLF4J = "1.7.32"
}

dependencies {
    implementation("org.junit.jupiter","junit-jupiter-api", Version.JUNIT)
    implementation("org.junit.jupiter", "junit-jupiter", Version.JUNIT)
    implementation("org.slf4j","slf4j-api",Version.SLF4J)
    implementation("org.slf4j","slf4j-simple",Version.SLF4J)
}

configure<JavaPluginExtension> {
   sourceCompatibility = JavaVersion.VERSION_11
}

tasks {
    withType(Test::class) {
        useJUnitPlatform()

        testLogging {
            displayGranularity = 2
            showStackTraces = false
            showExceptions = true
            showStandardStreams = false
            events("passed", "failed")
        }
    }
}
