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

val projectMetadata = buildMetadata<GradlePluginMetadata>(Developer)

group = Developer.namespace
version = projectMetadata.version

dependencies {
    implementation(libs.kotlin.serialization)
    implementation(libs.ktor.http)

    implementation(libs.kotlin.utils)
}

val javaVersion: Int = libs.versions.java.get().toInt()
setJavaCompatibilityVersion(javaVersion)
kotlin.jvmToolchain(javaVersion)

gradlePlugin.plugins.create(projectMetadata.name) {
    id = projectMetadata.id
    implementationClass = projectMetadata.implementationClass
}

publishing.repositories.maven {
    url = uri(Developer.getRepositoryUrl(projectMetadata.slug, true))
    credentials {
        username = Developer.username
        password = Secrets.githubPackagesToken
    }
}
