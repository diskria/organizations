package io.github.diskria.organizations.extensions

import com.github.gmazzo.buildconfig.BuildConfigExtension
import com.modrinth.minotaur.ModrinthExtension
import io.github.diskria.organizations.Secrets
import io.github.diskria.organizations.licenses.License
import io.github.diskria.organizations.licenses.MitLicense
import io.github.diskria.organizations.metadata.*
import io.github.diskria.organizations.minecraft.*
import io.github.diskria.organizations.minecraft.ModEnvironmentType.*
import io.github.diskria.organizations.minecraft.fabric.FabricModConfig
import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.delegates.toAutoNamedProperty
import io.github.diskria.utils.kotlin.extensions.asDirectoryOrNull
import io.github.diskria.utils.kotlin.extensions.common.failWithUnsupportedType
import io.github.diskria.utils.kotlin.extensions.common.unsupportedOperation
import io.github.diskria.utils.kotlin.extensions.setCase
import io.github.diskria.utils.kotlin.extensions.wrap
import io.github.diskria.utils.kotlin.poet.Property
import io.github.diskria.utils.kotlin.words.*
import kotlinx.serialization.json.Json
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.DuplicatesStrategy
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

typealias BaseExt = BasePluginExtension
typealias JavaExt = JavaPluginExtension
typealias KotlinExt = KotlinProjectExtension
typealias SourceSetsExt = SourceSetContainer
typealias GradlePluginExt = GradlePluginDevelopmentExtension
typealias PublishingExt = PublishingExtension
typealias SigningExt = SigningExtension
typealias BuildConfigExt = BuildConfigExtension
typealias FabricApiExt = FabricApiExtension
typealias LoomExt = LoomGradleExtensionAPI
typealias ModrinthExt = ModrinthExtension

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
    getExtensionOrThrow<BaseExt>("base").block()

fun <R> Project.java(block: Any.() -> R): R =
    getExtensionOrThrow<JavaExt>("java").block()

fun <R> Project.kotlin(block: Any.() -> R): R =
    getExtensionOrThrow<KotlinExt>("kotlin").block()

fun <R> Project.sourceSets(block: Any.() -> R): R =
    getExtensionOrThrow<SourceSetsExt>("java").block()

fun <R> Project.gradlePlugin(block: Any.() -> R): R =
    getExtensionOrThrow<GradlePluginExt>("maven-publish").block()

fun <R> Project.publishing(block: Any.() -> R): R =
    getExtensionOrThrow<PublishingExt>("publishing").block()

fun <R> Project.signing(block: Any.() -> R): R =
    getExtensionOrThrow<SigningExt>("signing").block()

fun <R> Project.buildConfig(block: Any.() -> R): R =
    getExtensionOrThrow<BuildConfigExt>("com.github.gmazzo.buildconfig").block()

fun <R> Project.fabricApi(block: Any.() -> R): R =
    getExtensionOrThrow<FabricApiExt>("fabric-loom").block()

fun <R> Project.loom(block: Any.() -> R): R =
    getExtensionOrThrow<LoomExt>("fabric-loom").block()

fun <R> Project.modrinth(block: Any.() -> R): R =
    getExtensionOrThrow<ModrinthExt>("com.modrinth.minotaur").block()

fun DependencyHandler.minecraft(dependencyNotation: Any): Dependency? =
    add("minecraft", dependencyNotation)

fun DependencyHandler.mappings(dependencyNotation: Any): Dependency? =
    add("mappings", dependencyNotation)

fun DependencyHandler.modImplementation(dependencyNotation: Any): Dependency? =
    add("modImplementation", dependencyNotation)

fun Project.versionCatalogs(): VersionCatalogsExtension =
    extensions.findByType(VersionCatalogsExtension::class.java) ?: unsupportedOperation()

fun Project.getCatalogVersion(name: String, catalog: String = "libs"): String? =
    versionCatalogs().named(catalog).findVersion(name).getOrNull()?.requiredVersion

fun Project.getCatalogVersionOrThrow(name: String, catalog: String = "libs"): String =
    getCatalogVersion(name, catalog) ?: gradleError(
        "Missing ${name.wrap(Constants.Char.SINGLE_QUOTE)} version in $catalog.versions.toml"
    )

fun Project.fileNames(projectPath: String): List<String> =
    layout
        .projectDirectory
        .dir(projectPath)
        .asFile
        .asDirectoryOrNull()
        ?.listFiles { it.isFile && !it.isHidden }
        ?.map { it.nameWithoutExtension }
        ?: emptyList()

fun Project.configureBuildConfig(packageName: String, className: String, fields: () -> List<Property<String>>) {
    buildConfig {
        this as BuildConfigExt
        packageName(packageName)
        className(className)
        fields().forEach { field ->
            buildConfigField(field.name, field.value)
        }
        useKotlinOutput {
            internalVisibility = false
            topLevelConstants = false
        }
    }
}

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

fun Project.configureProject(metadata: ProjectMetadata, sourceSetName: SourceSet = SourceSet.MAIN) {
    group = metadata.owner.namespace
    version = metadata.version
    base {
        this as BaseExt
        archivesName = metadata.slug
    }
    java {
        this as JavaExt
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(metadata.jdkVersion))
            vendor.set(JvmVendorSpec.ADOPTIUM)
            implementation.set(JvmImplementation.VENDOR_SPECIFIC)
        }
        withSourcesJar()
        withJavadocJar()
    }
    kotlin {
        this as KotlinExt
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
        this as SourceSetsExt
        named(sourceSetName.logicalName) {
            val generatedDirectory = "src/${sourceSetName}/generated"

            resources.srcDirs(generatedDirectory)
            java.srcDirs("$generatedDirectory/java")
        }
    }
}

fun Project.configureGradlePlugin(
    owner: Owner = DiskriaDeveloper,
    publishingTarget: PublishingTarget?,
    tags: Set<String> = emptySet(),
    license: License = MitLicense,
): ProjectMetadata {
    val metadata = buildMetadata(ProjectType.GRADLE_PLUGIN, owner, license)
    val id = metadata.packageName
    val className = "GradlePlugin"
    configureProject(metadata)
    gradlePlugin {
        this as GradlePluginExt
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
    minecraftVersion: String,
    environment: ModEnvironmentType,
    modLoader: ModLoader,
    isFabricApiRequired: Boolean,
    license: License = MitLicense,
): ProjectMetadata {
    requirePlugin("org.jetbrains.kotlin.plugin.serialization")
    requirePlugin("fabric-loom")
    val jvmTarget = MinecraftUtils.getRuntimeJavaVersion(minecraftVersion).toJvmTarget()
    val metadata = buildMetadata(ProjectType.MINECRAFT_MOD, MinecraftOrganization, license, jvmTarget)
    val modId = metadata.slug
    dependencies {
        minecraft("com.mojang:minecraft:$minecraftVersion")
        modImplementation("net.fabricmc:fabric-loader:${getCatalogVersionOrThrow("fabric-loader")}")

        val yarnFullVersion = getCatalogVersion("fabric-yarn-full")
        val yarnVersion = yarnFullVersion ?: "$minecraftVersion+build.${getCatalogVersionOrThrow("fabric-yarn")}"
        mappings("net.fabricmc:yarn:$yarnVersion:v2")

        val kotlinFullVersion = getCatalogVersion("fabric-kotlin-full")
        val kotlinVersion = kotlinFullVersion
            ?: "${getCatalogVersionOrThrow("fabric-kotlin")}+kotlin.${getCatalogVersionOrThrow("kotlin")}"
        modImplementation("net.fabricmc:fabric-language-kotlin:$kotlinVersion")

        if (isFabricApiRequired) {
            val apiFullVersion = getCatalogVersion("fabric-api-full")
            val apiVersion = apiFullVersion ?: "${getCatalogVersionOrThrow("fabric-api")}+$minecraftVersion"
            modImplementation("net.fabricmc.fabric-api:fabric-api:$apiVersion")
        }
    }
    configureBuildConfig(metadata.packageName, "ModMetadata") {
        val modId by modId.toAutoNamedProperty(ScreamingSnakeCase)
        val modName by metadata.name.toAutoNamedProperty(ScreamingSnakeCase)
        listOf(modId, modName)
    }
    val mainSourceSetDirectory = "src/${environment.getMainSourceSet()}"
    val packagePath = metadata.packageName.setCase(DotCase, PathCase)
    val dataGenerators = fileNames("$mainSourceSetDirectory/kotlin/$packagePath/generators").map {
        metadata.packageName + Constants.Char.DOT + it
    }
    val mixinClasses = environment.sourceSets.associateWith { sourceSet ->
        val pathBase = "src/${sourceSet.logicalName}/java/$packagePath/mixins"
        fileNames(
            when (sourceSet) {
                SourceSet.MAIN -> pathBase
                else -> "$pathBase/${sourceSet.logicalName}"
            }
        )
    }
    loom {
        this as LoomExt
        splitEnvironmentSourceSets()
        mods {
            create(modId) {
                sourceSets {
                    this as SourceSetsExt
                    environment.sourceSets.forEach { sourceSet ->
                        sourceSet(getByName(sourceSet.logicalName))
                    }
                }
            }
        }
        runs {
            ModSide.entries.forEach { side ->
                named(side.title) {
                    val hasSide = environment.sides.contains(side)
                    ideConfigGenerated(hasSide)

                    if (hasSide) {
                        name = side.runConfigurationName
                        runDir = "run/${side.title}"
                        when (side) {
                            ModSide.CLIENT -> client()
                            ModSide.SERVER -> server()
                        }
                        programArgs("--username", "${DiskriaDeveloper.name}-${side.title}")
                    }
                }
            }
        }
        accessWidenerPath.set(file("$mainSourceSetDirectory/resources/$modId.accesswidener"))
    }
    if (dataGenerators.isNotEmpty()) {
        loom {
            this as LoomExt
            runs {
                create("datagen") {
                    name = "Data Generation"
                    runDir = "build/fabricDataGeneration"
                    environment("server")
                    vmArgs(
                        "-Dfabric-api.datagen",
                        "-Dfabric-api.datagen.output-dir=${file("$mainSourceSetDirectory/generated")}",
                        "-Dfabric-api.datagen.modid=$modId",
                    )
                }
            }
        }
        fabricApi {
            this as FabricApiExt
            configureDataGeneration {
                client = true
            }
        }
    }
    val generateFabricModConfigTask by tasks.registering {
        val configFile = layout.buildDirectory.file("generated/resources/fabric/fabric.mod.json")
        outputs.file(configFile)
        doLast {
            configFile.get().asFile.apply {
                parentFile.mkdirs()
                writeText(
                    Json { prettyPrint = true }.encodeToString(
                        FabricModConfig.of(
                            metadata,
                            environment,
                            minecraftVersion,
                            getCatalogVersionOrThrow("fabric-loader"),
                            isFabricApiRequired,
                            dataGenerators,
                        )
                    )
                )
            }
        }
    }
    tasks.withType<ProcessResources>().configureEach {
        from(generateFabricModConfigTask)
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    configureProject(metadata, environment.getMainSourceSet())
    configurePublishing(metadata, PublishingTarget.MODRINTH)
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
        this as PublishingExt
        publications.withType<MavenPublication> {
            artifactId = metadata.slug
        }
        repositories {
            maven(metadata.owner.getRepositoryMavenUrl(metadata.slug)) {
                credentials {
                    username = DiskriaDeveloper.name
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
        this as PublishingExt
        repositories {
            maven(layout.buildDirectory.dir("staging-repo").get().asFile)
        }
    }
    val publication = publishing {
        this as PublishingExt
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
        this as SigningExt
        useInMemoryPgpKeys(gpgKey, gpgPassphrase)
        sign(publication)
    }
}

fun Project.configureModrinthPublishing(metadata: ProjectMetadata) {
    modrinth {
        this as ModrinthExt
        projectId.set(metadata.slug)
    }
}
