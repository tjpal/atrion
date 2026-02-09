plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlinxSerialization)

    `application`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(projects.atrionAdmin)

    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
}

application {
    mainClass = "dev.tjpal.atrion.admin.MainKt"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
