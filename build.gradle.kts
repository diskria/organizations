import io.github.diskria.organizations.Developer
import io.github.diskria.organizations.extensions.configureGradlePlugin
import io.github.diskria.organizations.publishing.PublishingTarget

plugins {
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlin.serialization)
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

configureGradlePlugin(Developer, libs.versions.java.get().toInt(), PublishingTarget.GITHUB_PACKAGES)
