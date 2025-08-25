package io.github.diskria.organizations.extensions

import com.modrinth.minotaur.ModrinthExtension
import io.github.diskria.organizations.Secrets
import io.github.diskria.organizations.exceptions.GradlePluginNotFoundException
import io.github.diskria.organizations.licenses.License
import io.github.diskria.organizations.licenses.MITLicense
import io.github.diskria.organizations.metadata.*
import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.capitalizeFirstChar
import io.github.diskria.utils.kotlin.extensions.common.unsupportedOperation
import io.github.diskria.utils.kotlin.extensions.setCase
import io.github.diskria.utils.kotlin.extensions.wrap
import io.github.diskria.utils.kotlin.words.CamelCase
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

fun Project.getCatalogVersionOrThrow(name: String, catalog: String = "libs"): String =
    getCatalogVersion(name, catalog)
        ?: throw GradleException("Missing ${name.wrap(Constants.Char.SINGLE_QUOTE)} version in $catalog.versions.toml")

fun Project.configureGradlePlugin(
    owner: Owner = Developer,
    publishingTarget: PublishingTarget?,
    tags: Set<String> = emptySet(),
    license: License = MITLicense(owner),
): ProjectMetadata {
    val metadata = configureProject(ProjectType.GRADLE_PLUGIN, owner, publishingTarget, license)
    gradlePlugin {
        website.set(metadata.url)
        vcsUrl.set("${metadata.url}.git")

        plugins.create(metadata.slug) {
            val pluginId = owner.namespace + Constants.Char.DOT + metadata.slug
            val className = metadata.slug.setCase(KebabCase, CamelCase).capitalizeFirstChar(true) + "GradlePlugin"

            id = pluginId
            implementationClass = pluginId + Constants.Char.DOT + className

            displayName = metadata.name
            description = metadata.description

            if (tags.isNotEmpty()) {
                this@create.tags.set(tags)
            }
        }
    }
    return metadata
}

fun Project.configureLibrary(license: License = MITLicense()): ProjectMetadata =
    configureProject(ProjectType.LIBRARY, LibrariesOrganization, PublishingTarget.MAVEN_CENTRAL)

fun Project.configureMinecraftMod(license: License = MITLicense()): ProjectMetadata =
    configureProject(ProjectType.MINECRAFT_MOD, MinecraftOrganization, PublishingTarget.MODRINTH)

fun Project.configureAndroidApp(license: License = MITLicense()): ProjectMetadata =
    configureProject(ProjectType.ANDROID_APP, AndroidOrganization, PublishingTarget.GOOGLE_PLAY)

fun Project.configureProject(
    type: ProjectType,
    owner: Owner,
    publishingTarget: PublishingTarget?,
    license: License = MITLicense(owner),
): ProjectMetadata {
    val metadata = buildMetadata(type, owner, license)
    group = owner.namespace
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
            ProjectType.GRADLE_PLUGIN -> "kotlin-dsl"
            ProjectType.ANDROID_APP -> "org.jetbrains.kotlin.android"
            else -> "org.jetbrains.kotlin.jvm"
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
            rename { oldName ->
                oldName + Constants.Char.UNDERSCORE + metadata.name
            }
        }
    }
    when (publishingTarget) {
        PublishingTarget.GITHUB_PACKAGES -> {
            Secrets.githubPackagesToken?.let { token ->
                publishing {
                    publications.withType<MavenPublication> {
                        artifactId = metadata.slug
                    }
                    repositories {
                        maven(owner.getRepositoryMavenUrl(metadata.slug)) {
                            credentials {
                                username = owner.name
                                password = token
                            }
                        }
                    }
                }
            }
        }

        PublishingTarget.MAVEN_CENTRAL -> configureMavenCentralPublishing(metadata)

        PublishingTarget.GRADLE_PLUGIN_PORTAL -> {

        }

        PublishingTarget.MODRINTH -> {

        }

        PublishingTarget.GOOGLE_PLAY -> {

        }

        else -> {
            if (publishingTarget != null) {
                throw GradleException("Unknown publishing target: $publishingTarget")
            }
            println("Publishing not configured, skip.")
        }
    }
    return metadata
}

private fun Project.configureMavenCentralPublishing(metadata: ProjectMetadata) {
    val key = Secrets.gpgKey
    val passphrase = Secrets.gpgPassphrase

    if (key.isNullOrBlank() || passphrase.isNullOrBlank()) {
        println("Skipping Maven Central publishing: GPG keys are missing")
        return
    }
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
                configureLicense(metadata.license)
                configureDevelopers(metadata.owner)
                scm {
                    url.set(metadata.url)
                    connection.set("scm:git:${metadata.owner.getRepositoryUrl(metadata.slug, true)}")
                    developerConnection.set(
                        "scm:git:git@github.com:${metadata.owner.getRepositoryPath(metadata.slug, true)}"
                    )
                }
            }
        }
    }
    signing {
        useInMemoryPgpKeys(Secrets.gpgKey, Secrets.gpgPassphrase)
        sign(publication)
    }
}

private fun Project.buildMetadata(type: ProjectType, owner: Owner, license: License): ProjectMetadata {
    val projectName: String by rootProject
    val projectDescription: String by rootProject
    val projectVersion: String by rootProject
    val slug = projectName.setCase(SpaceCase, KebabCase).lowercase()
    return ProjectMetadata(
        type = type,
        owner = owner,
        license = license,
        javaVersion = getCatalogVersionOrThrow("java").toInt(),
        name = projectName,
        description = projectDescription,
        version = projectVersion,
        slug = slug,
        url = owner.getRepositoryUrl(slug),
    )
}

private inline fun <reified T : Any> Project.getExtensionOrThrow(pluginId: String): T =
    extensions.findByType(T::class.java) ?: throw GradlePluginNotFoundException(pluginId)
