plugins {
    application
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
}

val ktorVersion = "3.3.1"
val serializationVersion = "1.9.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
}

application {
    mainClass.set("dev.tjpal.client.ClientKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}