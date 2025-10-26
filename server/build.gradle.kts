plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kapt)
    alias(libs.plugins.kotlinxSerialization)

    jacoco
    application
}

group = "dev.tjpal"
version = "1.0.0"
application {
    mainClass.set("dev.tjpal.MainKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.dagger)
    kapt(libs.kapt.dagger)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.mockk)
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}
tasks.jacocoTestReport {
    dependsOn(tasks.test)
}