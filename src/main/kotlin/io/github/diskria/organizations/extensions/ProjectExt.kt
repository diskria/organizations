package io.github.diskria.organizations.extensions

import com.github.gmazzo.buildconfig.BuildConfigExtension
import com.modrinth.minotaur.ModrinthExtension
import io.github.diskria.organizations.Secrets
import io.github.diskria.organizations.licenses.License
import io.github.diskria.organizations.licenses.MitLicense
import io.github.diskria.organizations.metadata.*
import io.github.diskria.organizations.minecraft.FabricModConfig
import io.github.diskria.organizations.minecraft.MinecraftUtils
import io.github.diskria.organizations.minecraft.ModEnvironment
import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.delegates.toAutoNamedProperty
import io.github.diskria.utils.kotlin.extensions.asDirectoryOrNull
import io.github.diskria.utils.kotlin.extensions.common.failWithUnsupportedType
import io.github.diskria.utils.kotlin.extensions.common.unsupportedOperation
import io.github.diskria.utils.kotlin.extensions.setCase
import io.github.diskria.utils.kotlin.extensions.wrap
import io.github.diskria.utils.kotlin.words.*
import kotlinx.serialization.json.Json
import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.jvm.optionals.getOrNull

fun Project.requirePlugin(id: String) {
    require(plugins.hasPlugin(id)) {
        gradleError("Plugin ${id.wrap(Constants.Char.SINGLE_QUOTE)} required but not applied.")
    }
}

inline fun <reified T : Any> Project.getExtensionOrThrow(pluginId: String): T {
    requirePlugin(pluginId)
    val clazz = T::class
    return extensions.findByType(clazz.java) ?: failWithUnsupportedType(clazz)
}

fun <R> Project.base(block: Any.() -> R): R =
    getExtensionOrThrow<BasePluginExtension>("base").block()

fun <R> Project.java(block: Any.() -> R): R =
    getExtensionOrThrow<JavaPluginExtension>("java").block()

fun <R> Project.kotlin(block: Any.() -> R): R =
    getExtensionOrThrow<KotlinProjectExtension>("kotlin").block()

fun <R> Project.sourceSets(block: Any.() -> R): R =
    getExtensionOrThrow<SourceSetContainer>("java").block()

fun <R> Project.gradlePlugin(block: Any.() -> R): R =
    getExtensionOrThrow<GradlePluginDevelopmentExtension>("maven-publish").block()

fun <R> Project.publishing(block: Any.() -> R): R =
    getExtensionOrThrow<PublishingExtension>("publishing").block()

fun <R> Project.signing(block: Any.() -> R): R =
    getExtensionOrThrow<SigningExtension>("signing").block()

fun <R> Project.buildConfig(block: Any.() -> R): R =
    getExtensionOrThrow<BuildConfigExtension>("com.github.gmazzo.buildconfig").block()

fun DependencyHandler.minecraft(dependencyNotation: Any): Dependency? =
    add("minecraft", dependencyNotation)

fun DependencyHandler.mappings(dependencyNotation: Any): Dependency? =
    add("mappings", dependencyNotation)

fun DependencyHandler.modImplementation(dependencyNotation: Any): Dependency? =
    add("modImplementation", dependencyNotation)

fun <R> Project.fabricApi(block: Any.() -> R): R =
    getExtensionOrThrow<FabricApiExtension>("fabric-loom").block()

fun <R> Project.modrinth(block: Any.() -> R): R =
    getExtensionOrThrow<ModrinthExtension>("com.modrinth.minotaur").block()

fun Project.versionCatalogs(): VersionCatalogsExtension =
    extensions.findByType(VersionCatalogsExtension::class.java) ?: unsupportedOperation()

fun Project.getCatalogVersion(name: String, catalog: String = "libs"): String? =
    versionCatalogs().named(catalog).findVersion(name).getOrNull()?.requiredVersion

fun Project.getCatalogVersionOrThrow(name: String, catalog: String = "libs"): String =
    getCatalogVersion(name, catalog) ?: gradleError(
        "Missing ${name.wrap(Constants.Char.SINGLE_QUOTE)} version in $catalog.versions.toml"
    )

fun Project.buildMetadata(
    type: ProjectType,
    owner: Owner,
    license: License,
    jvmTarget: JvmTarget? = null,
): ProjectMetadata {
    val projectName: String by rootProject
    val projectDescription: String by rootProject
    val projectVersion: String by rootProject
    val jdkVersion = getCatalogVersionOrThrow("java").toInt()
    return ProjectMetadata(
        owner = owner,
        type = type,
        license = license,
        name = projectName,
        description = projectDescription,
        version = projectVersion,
        slug = projectName.setCase(SpaceCase, KebabCase).lowercase(),
        packageName = owner.namespace + Constants.Char.DOT + projectName.setCase(SpaceCase, DotCase).lowercase(),
        jdkVersion = jdkVersion,
        jvmTarget = jvmTarget ?: jdkVersion.toJvmTarget(),
    )
}

fun Project.configureProject(metadata: ProjectMetadata) {
    group = metadata.owner.namespace
    version = metadata.version
    base {
        this as BasePluginExtension
        archivesName = metadata.slug
    }
    java {
        this as JavaPluginExtension
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(metadata.jdkVersion))
            vendor.set(JvmVendorSpec.ADOPTIUM)
            implementation.set(JvmImplementation.VENDOR_SPECIFIC)
        }
        withSourcesJar()
        withJavadocJar()
    }
    kotlin {
        this as KotlinProjectExtension
        jvmToolchain(metadata.jdkVersion)
    }
    tasks.withType<JavaCompile>().configureEach {
        with(options) {
            release.set(metadata.jvmTarget.toInt())
            encoding = Charsets.UTF_8.toString()
        }
    }
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(metadata.jvmTarget)
        }
    }
    tasks.named<Jar>("jar") {
        from("LICENSE") {
            rename { oldName ->
                oldName + Constants.Char.UNDERSCORE + metadata.slug
            }
        }
    }
    sourceSets {
        this as SourceSetContainer
        named("main") {
            resources.srcDirs("src/main/generated")
            java.srcDirs("src/main/generated/java")
        }
    }
}

fun Project.configureGradlePlugin(
    owner: Owner = Developer,
    publishingTarget: PublishingTarget?,
    tags: Set<String> = emptySet(),
    license: License = MitLicense,
): ProjectMetadata {
    val metadata = buildMetadata(ProjectType.GRADLE_PLUGIN, owner, license)
    val id = metadata.packageName
    val className = "GradlePlugin"
    configureProject(metadata)
    gradlePlugin {
        this as GradlePluginDevelopmentExtension
        website.set(owner.getRepositoryUrl(metadata.slug))
        vcsUrl.set(owner.getRepositoryUrl(metadata.slug, isVcsUrl = true))
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
    configurePublishing(metadata, publishingTarget)
    return metadata
}

fun Project.configureLibrary(license: License = MitLicense): ProjectMetadata {
    val metadata = buildMetadata(ProjectType.LIBRARY, LibrariesOrganization, license)
    configureProject(metadata)
    configurePublishing(metadata, PublishingTarget.MAVEN_CENTRAL)
    return metadata
}

fun Project.configureMinecraftMod(
    targetVersion: String,
    environment: ModEnvironment,
    isFabricApiRequired: Boolean,
    license: License = MitLicense,
): ProjectMetadata {
    requirePlugin("org.jetbrains.kotlin.plugin.serialization")
    requirePlugin("fabric-loom")
    val jvmTarget = MinecraftUtils.getRuntimeJavaVersion(targetVersion).toJvmTarget()
    val metadata = buildMetadata(ProjectType.MINECRAFT_MOD, MinecraftOrganization, license, jvmTarget)
    configureProject(metadata)
    dependencies {
        minecraft("com.mojang:minecraft:$targetVersion")
        modImplementation("net.fabricmc:fabric-loader:${getCatalogVersionOrThrow("fabric-loader")}")

        val fullYarnVersion = getCatalogVersion("fabric-yarn-full")
        val yarnVersion = fullYarnVersion ?: "$targetVersion+build.${getCatalogVersionOrThrow("fabric-yarn")}"
        mappings("net.fabricmc:yarn:$yarnVersion:v2")

        if (isFabricApiRequired) {
            val fullVersion = getCatalogVersion("fabric-api-full")
            val version = fullVersion ?: "${getCatalogVersionOrThrow("fabric-api")}+$targetVersion"
            modImplementation("net.fabricmc.fabric-api:fabric-api:$version")
        }
    }
    buildConfig {
        this as BuildConfigExtension
        packageName(metadata.packageName)
        className("ModMetadata")
        val modId by metadata.slug.toAutoNamedProperty(ScreamingSnakeCase)
        val modName by metadata.name.toAutoNamedProperty(ScreamingSnakeCase)
        listOf(modId, modName).forEach {
            buildConfigField(it.name, it.value)
        }
    }
    val dataGenerators = layout.projectDirectory
        .dir("src/client/kotlin/${metadata.packageName.setCase(DotCase, PathCase)}/generators")
        .asFile
        .asDirectoryOrNull()
        ?.listFiles { it.isFile && !it.isHidden }
        ?.map { metadata.packageName + Constants.Char.DOT + it.nameWithoutExtension }
        ?: emptyList()
    if (dataGenerators.isNotEmpty()) {
        fabricApi {
            this as FabricApiExtension
            configureDataGeneration {
                client = true
            }
        }
    }
    val generateFabricModConfigTask by tasks.registering {
        val output = layout.buildDirectory.file("src/main/generated/fabric/fabric.mod.json")
        outputs.file(output)
        doLast {
            output.get().asFile.apply {
                parentFile.mkdirs()
                writeText(
                    Json { prettyPrint = true }.encodeToString(
                        FabricModConfig.of(
                            metadata,
                            environment,
                            targetVersion,
                            getCatalogVersionOrThrow("fabric-loader"),
                            isFabricApiRequired,
                            dataGenerators,
                        )
                    )
                )
            }
        }
    }
    tasks.named<ProcessResources>("processResources") {
        from(generateFabricModConfigTask)
    }
    configureModrinthPublishing(metadata)
    return metadata
}

fun Project.configureAndroidApp(license: License = MitLicense): ProjectMetadata {
    val metadata = buildMetadata(ProjectType.ANDROID_APP, AndroidOrganization, license)
    configureProject(metadata)
    configurePublishing(metadata, PublishingTarget.GOOGLE_PLAY)
    return metadata
}

fun Project.configurePublishing(metadata: ProjectMetadata, target: PublishingTarget?) {
    if (target == null) {
        println("Publishing target is null, skip.")
        return
    }
    when (target) {
        PublishingTarget.GITHUB_PACKAGES -> configureGithubPackagesPublishing(metadata)
        PublishingTarget.MAVEN_CENTRAL -> configureMavenCentralPublishing(metadata)
        PublishingTarget.MODRINTH -> configureModrinthPublishing(metadata)
        PublishingTarget.GRADLE_PLUGIN_PORTAL -> {}
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
        this as PublishingExtension
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
        this as PublishingExtension
        repositories {
            maven(layout.buildDirectory.dir("staging-repo").get().asFile)
        }
    }
    val publication = publishing {
        this as PublishingExtension
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
        this as SigningExtension
        useInMemoryPgpKeys(gpgKey, gpgPassphrase)
        sign(publication)
    }
}

fun Project.configureModrinthPublishing(metadata: ProjectMetadata) {
    modrinth {
        this as ModrinthExtension
        projectId.set(metadata.slug)
    }
}
