plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kapt)

    application
}

group = "dev.tjpal"
version = "1.0.0"
application {
    mainClass.set("dev.tjpal.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)

    implementation(libs.dagger)
    kapt(libs.kapt.dagger)

    testImplementation(libs.kotlin.testJunit)
}