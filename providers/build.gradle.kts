group = "com.jamhour"
version = "1.0-SNAPSHOT"

plugins {
    val kotlinVersion = "2.0.20"
    kotlin("plugin.serialization") version kotlinVersion
}

dependencies {
    val jsoupVersion = "1.18.1"

    implementation(project(":core"))
    implementation(project(":util"))
    implementation("org.jsoup:jsoup:${jsoupVersion}") // https://mvnrepository.com/artifact/org.jsoup/jsoup
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}