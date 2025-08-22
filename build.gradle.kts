import io.github.diskria.organizations.Developer
import io.github.diskria.organizations.Secrets
import io.github.diskria.organizations.extensions.setJavaCompatibilityVersion
import io.github.diskria.utils.kotlin.extensions.appendPackageName
import io.github.diskria.utils.kotlin.extensions.setCase
import io.github.diskria.utils.kotlin.words.KebabCase
import io.github.diskria.utils.kotlin.words.SpaceCase

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

private val plugin = object {
    val projectName: String by rootProject
    val projectVersion: String by rootProject
    val slug: String = projectName.setCase(SpaceCase, KebabCase).lowercase()
    val id: String = Developer.namespace.appendPackageName(slug)
}

group = Developer.namespace
version = plugin.projectVersion

dependencies {
    implementation(libs.kotlin.serialization)
    implementation(libs.ktor.http)

    implementation(libs.kotlin.utils)
}

val javaVersion: Int = libs.versions.java.get().toInt()
setJavaCompatibilityVersion(javaVersion)
kotlin.jvmToolchain(javaVersion)

gradlePlugin.plugins.create(plugin.projectName) {
    id = plugin.id
    implementationClass = plugin.id.appendPackageName("OrganizationsPlugin")
}

publishing.repositories.maven {
    url = uri(Developer.getRepositoryUrl(plugin.slug, true))
    credentials {
        username = Developer.username
        password = Secrets.githubPackagesToken
    }
}
