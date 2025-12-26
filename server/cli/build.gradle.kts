plugins {
    application
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinxSerialization)
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation(libs.kotlinx.serialization.json)
    implementation(project(":server"))
}

application {
    mainClass.set("dev.tjpal.cli.CreateSecretCliKt")
}