plugins {
    val kotlinVersion = "2.0.20"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.jamhour"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val coroutinesVersion = "1.9.0-RC.2"
    val serializationVersion = "1.7.2"
    val jsoupVersion = "1.18.1"
    val kotlinVersion = "2.0.20"

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("org.jsoup:jsoup:$jsoupVersion") // https://mvnrepository.com/artifact/org.jsoup/jsoup
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    // Create a fat JAR with dependencies
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveClassifier = "all"
    }
}

kotlin {
    jvmToolchain(21)
}
