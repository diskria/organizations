import io.github.diskria.organizations.Developer
import io.github.diskria.organizations.extensions.configureGradlePlugin
import io.github.diskria.organizations.extensions.configureJava
import io.github.diskria.organizations.publishing.PublishingTarget

plugins {
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization)
    implementation(libs.ktor.http)
    implementation(libs.kotlin.utils)
}

configureJava(libs.versions.java.get().toInt())

configureGradlePlugin(Developer, PublishingTarget.GITHUB_PACKAGES)
