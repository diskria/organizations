import io.github.diskria.organizations.extensions.configureGradlePlugin
import io.github.diskria.organizations.metadata.Developer
import io.github.diskria.organizations.metadata.PublishingTarget

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    compileOnly(libs.build.config.gradle.plugin)
    compileOnly(libs.modrinth.minotaur.gradle.plugin)

    implementation(libs.ktor.http)
    implementation(libs.kotlin.utils)
    implementation(libs.kotlin.serialization)

    constraints {
        // Override vulnerable transitive dependency (Okio < 3.4.0, CVE-2023-3635)
        // com.modrinth.minotaur → Modrinth4J → Okio
        implementation(libs.okio)
    }
}

configureGradlePlugin(Developer, PublishingTarget.GITHUB_PACKAGES)
