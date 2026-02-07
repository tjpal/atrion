plugins {
    alias(libs.plugins.kotlinJvm)

    `maven-publish`
}

group = "dev.tjpal.atrion.client"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("atrion-client") {
            from(components["kotlin"])
            groupId = project.group.toString()
            artifactId = "atrion-client"
            version = project.version.toString()
        }
    }
    repositories {
        maven {
            name = "local"
            mavenLocal()
        }
        maven {
            name = "github"
            url = uri("https://maven.pkg.github.com/tjpal/atrion")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks.register<PublishToMavenRepository>("publishToLocal") {
    publication = publishing.publications["atrion-client"] as MavenPublication
    repository = publishing.repositories["local"] as MavenArtifactRepository
}

tasks.register<PublishToMavenRepository>("publishToGithub") {
    publication = publishing.publications["atrion-client"] as MavenPublication
    repository = publishing.repositories["github"] as MavenArtifactRepository
}