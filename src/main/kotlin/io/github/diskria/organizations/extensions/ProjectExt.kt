package io.github.diskria.organizations.extensions

import com.github.gmazzo.buildconfig.BuildConfigExtension
import com.modrinth.minotaur.ModrinthExtension
import io.github.diskria.organizations.Secrets
import io.github.diskria.organizations.exceptions.GradlePluginNotFoundException
import io.github.diskria.organizations.licenses.License
import io.github.diskria.organizations.licenses.MitLicense
import io.github.diskria.organizations.metadata.*
import io.github.diskria.organizations.minecraft.FabricModConfig
import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.delegates.toAutoNamedProperty
import io.github.diskria.utils.kotlin.extensions.capitalizeFirstChar
import io.github.diskria.utils.kotlin.extensions.common.unsupportedOperation
import io.github.diskria.utils.kotlin.extensions.setCase
import io.github.diskria.utils.kotlin.extensions.wrap
import io.github.diskria.utils.kotlin.words.CamelCase
import io.github.diskria.utils.kotlin.words.KebabCase
import io.github.diskria.utils.kotlin.words.ScreamingSnakeCase
import io.github.diskria.utils.kotlin.words.SpaceCase
import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import java.nio.charset.StandardCharsets
import kotlin.jvm.optionals.getOrNull

fun <R> Project.base(block: BasePluginExtension.() -> R): R =
    getExtensionOrThrow<BasePluginExtension>("base").block()

fun <R> Project.java(block: JavaPluginExtension.() -> R): R =
    getExtensionOrThrow<JavaPluginExtension>("java").block()

fun <R> Project.sourceSets(block: SourceSetContainer.() -> R): R =
    getExtensionOrThrow<SourceSetContainer>("java").block()

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

fun <R> Project.buildConfig(block: BuildConfigExtension.() -> R): R =
    getExtensionOrThrow<BuildConfigExtension>("com.github.gmazzo.buildconfig").block()

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
    license: License = MitLicense(owner),
): ProjectMetadata {
    val metadata = configureProject(ProjectType.GRADLE_PLUGIN, owner, publishingTarget, license)
    gradlePlugin {
        website.set(owner.getRepositoryUrl(metadata.slug))
        vcsUrl.set(owner.getRepositoryUrl(metadata.slug, isVcsUrl = true))

        val id = metadata.packageName
        val className = metadata.slug.setCase(KebabCase, CamelCase).capitalizeFirstChar(true) + "GradlePlugin"
        plugins.create(id) {
            this.id = id
            implementationClass = id + Constants.Char.DOT + className

            displayName = metadata.name
            description = metadata.description

            if (tags.isNotEmpty()) {
                this.tags.set(tags)
            }
        }
    }
    return metadata
}

fun Project.configureLibrary(license: License = MitLicense()): ProjectMetadata =
    configureProject(ProjectType.LIBRARY, LibrariesOrganization, PublishingTarget.MAVEN_CENTRAL)

fun Project.configureMinecraftMod(license: License = MitLicense()): ProjectMetadata {
    val metadata = configureProject(ProjectType.MINECRAFT_MOD, MinecraftOrganization, PublishingTarget.MODRINTH)
    buildConfig {
        packageName(metadata.packageName)
        className("ModMetadata")

        val modId by metadata.slug.toAutoNamedProperty(ScreamingSnakeCase)
        val modName by metadata.name.toAutoNamedProperty(ScreamingSnakeCase)

        listOf(modId, modName).forEach {
            buildConfigField(it.name, it.value)
        }
    }
    tasks.named<ProcessResources>("processResources") {
        layout.buildDirectory.resolve("src/main/generated/fabric/fabric.mod.json").apply {
            parentFile.mkdirs()
            writeText(Json { prettyPrint = true }.encodeToString(FabricModConfig.of(metadata)))
        }
    }
    return metadata
}

fun Project.configureAndroidApp(license: License = MitLicense()): ProjectMetadata =
    configureProject(ProjectType.ANDROID_APP, AndroidOrganization, PublishingTarget.GOOGLE_PLAY)

fun Project.configureProject(
    type: ProjectType,
    owner: Owner,
    publishingTarget: PublishingTarget?,
    license: License = MitLicense(owner),
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

    sourceSets {
        named("main") {
            resources.srcDirs("src/main/generated")
            java.srcDirs("src/main/generated/java")
        }
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
                oldName + Constants.Char.UNDERSCORE + metadata.slug
            }
        }
    }
    configurePublishing(metadata, publishingTarget)
    return metadata
}

private fun Project.configurePublishing(metadata: ProjectMetadata, target: PublishingTarget?) {
    if (target == null) {
        println("Publishing target is null, skip.")
        return
    }
    when (target) {
        PublishingTarget.GITHUB_PACKAGES -> configureGithubPackagesPublishing(metadata)
        PublishingTarget.MAVEN_CENTRAL -> configureMavenCentralPublishing(metadata)
        PublishingTarget.GRADLE_PLUGIN_PORTAL -> {}
        PublishingTarget.MODRINTH -> configureModrinthPublishing(metadata)
        PublishingTarget.GOOGLE_PLAY -> {}
    }
}

private fun Project.configureGithubPackagesPublishing(metadata: ProjectMetadata) {
    val githubPackagesToken = Secrets.githubPackagesToken
    if (githubPackagesToken == null) {
        println("Skipping Github Packages publishing configuration: token is missing")
        return
    }
    publishing {
        publications.withType<MavenPublication> {
            artifactId = metadata.slug
        }
        repositories {
            maven(metadata.owner.getRepositoryMavenUrl(metadata.slug)) {
                credentials {
                    username = Developer.name
                    password = githubPackagesToken
                }
            }
        }
    }
}

private fun Project.configureMavenCentralPublishing(metadata: ProjectMetadata) {
    val gpgKey = Secrets.gpgKey
    val gpgPassphrase = Secrets.gpgPassphrase
    if (gpgKey == null || gpgPassphrase == null) {
        println("Skipping Maven Central publishing configuration: GPG keys are missing")
        return
    }
    publishing {
        repositories {
            maven(layout.buildDirectory.resolve("staging-repo"))
        }
    }
    val publication = publishing {
        publications.create<MavenPublication>(metadata.slug) {
            artifactId = metadata.slug
            from(components["java"])
            pom {
                name.set(metadata.name)
                description.set(metadata.description)
                url.set(metadata.owner.getRepositoryUrl(metadata.slug))
                licenses {
                    license {
                        metadata.license.let {
                            name.set(it.displayName)
                            url.set(it.getUrl())
                        }
                    }
                }
                developers {
                    developer {
                        metadata.owner.name.let {
                            id.set(it)
                            name.set(it)
                        }
                        email.set(metadata.owner.email)
                    }
                }
                scm {
                    url.set(metadata.owner.getRepositoryUrl(metadata.slug))
                    connection.set(
                        metadata.scm.buildUri(metadata.owner.getRepositoryUrl(metadata.slug, isVcsUrl = true))
                    )
                    developerConnection.set(
                        metadata.scm.buildUri(
                            SoftwareForgeType.GITHUB.getSshAuthority(),
                            metadata.owner.getRepositoryPath(metadata.slug, isVcsUrl = true)
                        )
                    )
                }
            }
        }
    }
    signing {
        useInMemoryPgpKeys(gpgKey, gpgPassphrase)
        sign(publication)
    }
}

private fun Project.configureModrinthPublishing(metadata: ProjectMetadata) {
    modrinth {

    }
}

private fun Project.buildMetadata(type: ProjectType, owner: Owner, license: License): ProjectMetadata {
    val projectName: String by rootProject
    val projectDescription: String by rootProject
    val projectVersion: String by rootProject
    val slug = projectName.setCase(SpaceCase, KebabCase).lowercase()
    return ProjectMetadata(
        owner = owner,
        type = type,
        license = license,

        name = projectName,
        description = projectDescription,
        version = projectVersion,
        slug = slug,
        packageName = owner.namespace + Constants.Char.DOT + slug,

        javaVersion = getCatalogVersionOrThrow("java").toInt(),
    )
}

private inline fun <reified T : Any> Project.getExtensionOrThrow(pluginId: String): T =
    extensions.findByType(T::class.java) ?: throw GradlePluginNotFoundException(pluginId)
