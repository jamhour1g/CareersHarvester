plugins {
    kotlin("jvm") version "2.0.20-RC2"
    kotlin("plugin.serialization") version "2.0.0"
}

group = "com.jamhour"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val coroutinesVersion = "1.9.0-RC"
    val serializationVersion = "1.7.1"
    val jsoupVersion = "1.18.1"
    val kotlinStdLibVersion = "2.0.10"

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("org.jsoup:jsoup:$jsoupVersion") // https://mvnrepository.com/artifact/org.jsoup/jsoup
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinStdLibVersion")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}