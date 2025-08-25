import io.github.diskria.organizations.metadata.Developer
import io.github.diskria.organizations.metadata.PublishingTarget
import io.github.diskria.organizations.extensions.configureGradlePlugin

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

dependencies {
    implementation(libs.ktor.http)
    implementation(libs.kotlin.utils)

    implementation(libs.kotlin.gradle.plugin)
    compileOnly(libs.modrinth.minotaur)

    constraints {
        // Override vulnerable transitive dependency (Okio < 3.4.0, CVE-2023-3635)
        // Minotaur → Modrinth4J → OkHttp/Okio
        implementation(libs.okio)
    }
}

configureGradlePlugin(Developer, PublishingTarget.GITHUB_PACKAGES)
