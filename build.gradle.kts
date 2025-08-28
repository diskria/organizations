import io.github.diskria.organizations.extensions.configureGradlePlugin
import io.github.diskria.organizations.metadata.DiskriaDeveloper
import io.github.diskria.organizations.metadata.PublishingTarget

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlin.plugin)
    compileOnly(libs.build.config.plugin)
    compileOnly(libs.modrinth.minotaur.plugin)
    compileOnly(libs.fabric.loom.plugin)

    implementation(libs.ktor.http)
    implementation(libs.kotlin.utils)
    implementation(libs.kotlin.serialization)

    constraints {
        // Override vulnerable transitive dependency (Okio < 3.4.0, CVE-2023-3635)
        // com.modrinth.minotaur → Modrinth4J → Okio
        implementation(libs.okio)
    }
}

configureGradlePlugin(DiskriaDeveloper, PublishingTarget.GITHUB_PACKAGES)
