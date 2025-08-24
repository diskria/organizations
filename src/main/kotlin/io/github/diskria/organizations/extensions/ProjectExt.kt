package io.github.diskria.organizations.extensions

import com.modrinth.minotaur.ModrinthExtension
import io.github.diskria.organizations.*
import io.github.diskria.organizations.ProjectType.*
import io.github.diskria.organizations.exceptions.GradlePluginNotFoundException
import io.github.diskria.organizations.licenses.LicenseType
import io.github.diskria.organizations.publishing.PublishingTarget
import io.github.diskria.organizations.publishing.PublishingTarget.*
import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.appendPackageName
import io.github.diskria.utils.kotlin.extensions.capitalizeFirstChar
import io.github.diskria.utils.kotlin.extensions.common.unsupportedOperation
import io.github.diskria.utils.kotlin.extensions.setCase
import io.github.diskria.utils.kotlin.words.KebabCase
import io.github.diskria.utils.kotlin.words.SpaceCase
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import java.nio.charset.StandardCharsets
import kotlin.jvm.java
import kotlin.jvm.optionals.getOrNull

fun <R> Project.base(block: BasePluginExtension.() -> R): R =
    getExtensionOrThrow<BasePluginExtension>("base").block()

fun <R> Project.java(block: JavaPluginExtension.() -> R): R =
    getExtensionOrThrow<JavaPluginExtension>("java").block()

fun <R> Project.kotlin(pluginId: String, block: KotlinProjectExtension.() -> R): R =
    getExtensionOrThrow<KotlinProjectExtension>(pluginId).block()

fun <R> Project.gradlePlugin(block: GradlePluginDevelopmentExtension.() -> R): R =
    getExtensionOrThrow<GradlePluginDevelopmentExtension>("java-gradle-plugin").block()

fun <R> Project.publishing(block: PublishingExtension.() -> R): R =
    getExtensionOrThrow<PublishingExtension>("maven-publish").block()

fun <R> Project.signing(block: SigningExtension.() -> R): R =
    getExtensionOrThrow<SigningExtension>("signing").block()

fun <R> Project.modrinth(block: ModrinthExtension.() -> R): R =
    getExtensionOrThrow<ModrinthExtension>("com.modrinth.minotaur").block()

fun Project.versionCatalogs(): VersionCatalogsExtension =
    extensions.findByType(VersionCatalogsExtension::class.java) ?: unsupportedOperation()

fun Project.getCatalogVersion(name: String, catalog: String = "libs"): String? =
    versionCatalogs().named(catalog).findVersion(name).getOrNull()?.requiredVersion

fun Project.configureGradlePlugin(owner: Owner = Developer, publishingTarget: PublishingTarget?) {
    val metadata = configureProject(GRADLE_PLUGIN, owner, publishingTarget)
    gradlePlugin {
        plugins.create(metadata.slug) {
            id = metadata.owner.namespace.appendPackageName(metadata.slug)
            implementationClass = id.appendPackageName(metadata.slug.capitalizeFirstChar(true) + "GradlePlugin")
        }
    }
}

fun Project.configureLibrary() {
    configureProject(LIBRARY, LibrariesOrganization, MAVEN_CENTRAL)
}

fun Project.configureMinecraftMod() {
    configureProject(MINECRAFT_MOD, MinecraftOrganization, MODRINTH)
}

fun Project.configureAndroidApp() {
    configureProject(ANDROID_APP, AndroidOrganization, GOOGLE_PLAY)
}

fun Project.configureProject(type: ProjectType, owner: Owner, publishingTarget: PublishingTarget?): ProjectMetadata {
    val javaVersion = getCatalogVersion("java")?.toInt()
        ?: throw GradleException("Missing `java` version in libs.versions.toml")

    val projectName: String by rootProject
    val projectDescription: String by rootProject
    val projectVersion: String by rootProject
    val slug = projectName.setCase(SpaceCase, KebabCase).lowercase()
    val metadata = ProjectMetadata(
        type = type,
        javaVersion = javaVersion,
        owner = owner,
        name = projectName,
        description = projectDescription,
        version = projectVersion,
        slug = slug,
        url = owner.getRepositoryUrl(slug),
    )

    group = metadata.owner.namespace
    version = metadata.version
    base {
        archivesName = metadata.slug
    }
    java {
        JavaVersion.toVersion(metadata.javaVersion).let { javaVersion ->
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
        withSourcesJar()
        withJavadocJar()
    }
    kotlin(
        when (metadata.type) {
            GRADLE_PLUGIN -> "kotlin-dsl"
            LIBRARY, MINECRAFT_MOD -> "org.jetbrains.kotlin.jvm"
            ANDROID_APP -> "org.jetbrains.kotlin.android"
        }
    ) {
        jvmToolchain(metadata.javaVersion)
    }
    tasks.withType<JavaCompile>().configureEach {
        with(options) {
            release.set(metadata.javaVersion)
            encoding = StandardCharsets.UTF_8.name()
        }
    }
    tasks.named<Jar>("jar") {
        from("LICENSE") {
            rename { it + Constants.Char.UNDERSCORE + metadata.name }
        }
    }
    if (publishingTarget != null) {
        when (publishingTarget) {
            GITHUB_PACKAGES -> configureGithubPackagesPublishing(metadata)
            MAVEN_CENTRAL -> configureMavenCentralPublishing(metadata)
            GRADLE_PLUGIN_PORTAL -> configureGithubPackagesPublishing(metadata)
            MODRINTH -> configureModrinthPublishing(metadata)
            GOOGLE_PLAY -> configureGooglePlayPublishing(metadata)
        }
    }
    return metadata
}

private fun Project.configureGithubPackagesPublishing(metadata: ProjectMetadata) {
    publishing {
        publications.withType<MavenPublication> {
            artifactId = metadata.slug
        }
        repositories.maven {
            url = uri(metadata.owner.getRepositoryUrl(metadata.slug, isMaven = true))
            credentials {
                username = metadata.owner.name
                password = Secrets.githubPackagesToken
            }
        }
    }
}

private fun Project.configureMavenCentralPublishing(metadata: ProjectMetadata) {
    publishing {
        repositories {
            maven {
                url = layout.buildDirectory.dir("staging-repo").get().asFile.toURI()
            }
        }
    }
    val publication = publishing {
        publications.create<MavenPublication>(metadata.slug) {
            artifactId = metadata.slug
            from(components["java"])
            pom {
                name.set(metadata.name)
                description.set(metadata.description)
                url.set(metadata.url)
                configureLicense(LicenseType.MIT)
                configureDevelopers(metadata.owner)
                scm {
                    url.set(metadata.url)
                    connection.set("scm:git:${metadata.url}.git")
                    developerConnection.set(
                        "scm:git:git@github.com:${metadata.owner.getRepositoryPath(metadata.slug)}.git"
                    )
                }
            }
        }
    }
    val gpgKey = System.getenv("GPG_KEY")
    val gpgPassphrase = System.getenv("GPG_PASSPHRASE")
    if (!gpgKey.isNullOrBlank() && !gpgPassphrase.isNullOrBlank()) {
        signing {
            useInMemoryPgpKeys(gpgKey, gpgPassphrase)
            sign(publication)
        }
    }
}

private fun Project.configureGradlePluginPortalPublishing(metadata: ProjectMetadata) {

}

private fun Project.configureModrinthPublishing(metadata: ProjectMetadata) {

}

private fun Project.configureGooglePlayPublishing(metadata: ProjectMetadata) {

}

private inline fun <reified T : Any> Project.getExtensionOrThrow(pluginId: String): T =
    extensions.findByType(T::class.java) ?: throw GradlePluginNotFoundException(pluginId)
