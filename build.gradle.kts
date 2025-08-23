import io.github.diskria.organizations.Developer
import io.github.diskria.organizations.GradlePluginMetadata
import io.github.diskria.organizations.Secrets
import io.github.diskria.organizations.extensions.buildMetadata
import io.github.diskria.organizations.extensions.setJavaCompatibilityVersion

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

val gradlePluginMetadata = buildMetadata<GradlePluginMetadata>(Developer)

group = Developer.namespace
version = gradlePluginMetadata.version

dependencies {
    implementation(libs.kotlin.serialization)
    implementation(libs.ktor.http)

    implementation(libs.kotlin.utils)
}

val javaVersion: Int = libs.versions.java.get().toInt()
setJavaCompatibilityVersion(javaVersion)
kotlin.jvmToolchain(javaVersion)

gradlePlugin.plugins.create(gradlePluginMetadata.name) {
    id = gradlePluginMetadata.id
    implementationClass = gradlePluginMetadata.implementationClass
}

publishing.repositories.maven {
    url = uri(Developer.getRepositoryUrl(gradlePluginMetadata.slug, true))
    credentials {
        username = Developer.username
        password = Secrets.githubPackagesToken
    }
}
