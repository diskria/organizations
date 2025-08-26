package io.github.diskria.organizations.minecraft

import io.github.diskria.organizations.extensions.toInt
import io.github.diskria.organizations.metadata.Developer
import io.github.diskria.organizations.metadata.ProjectMetadata
import io.github.diskria.utils.kotlin.extensions.appendPackageName
import io.github.diskria.utils.kotlin.extensions.common.fileName
import io.github.diskria.utils.kotlin.extensions.setCase
import io.github.diskria.utils.kotlin.words.FlatCase
import io.github.diskria.utils.kotlin.words.SpaceCase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class FabricModConfig(
    val schemaVersion: Int,
    val id: String,
    val version: String,
    val name: String,
    val description: String,
    val authors: List<String>,
    val license: String,
    val icon: String,
    val environment: String,
    val mixins: List<String>,
    val accessWidener: String,

    @SerialName("contact")
    val links: Links,

    @SerialName("entrypoints")
    val entryPoints: EntryPoints,

    @SerialName("depends")
    val dependencies: Dependencies,
) {
    @Serializable
    class Links private constructor(
        val homepage: String,
        val sources: String,
    ) {
        companion object {
            fun of(sources: String, homepage: String = sources): Links =
                Links(
                    homepage = homepage,
                    sources = sources,
                )
        }
    }

    @Serializable
    data class Dependencies(
        @SerialName("java")
        val jvmDependency: String,

        @SerialName("minecraft")
        val minecraftDependency: String,

        @SerialName("fabricloader")
        val loaderDependency: String,

        @SerialName("fabric-api")
        val apiDependency: String? = null,
    ) {
        companion object {
            fun of(
                javaVersion: Int,
                minecraftVersion: String,
                loaderVersion: String,
                isApiRequired: Boolean,
            ): Dependencies =
                Dependencies(
                    jvmDependency = ">=$javaVersion",
                    minecraftDependency = ">=$minecraftVersion",
                    loaderDependency = ">=$loaderVersion",
                    apiDependency = if (isApiRequired) "*" else null,
                )
        }
    }

    @Serializable
    data class EntryPoints(
        @SerialName("client")
        val clientEntryPoints: List<EntryPoint>? = null,

        @SerialName("server")
        val serverEntryPoints: List<EntryPoint>? = null,

        @SerialName("main")
        val mainEntryPoints: List<EntryPoint>? = null,

        @SerialName("fabric-datagen")
        val dataGenerators: List<EntryPoint>? = null,
    ) {
        companion object {
            fun of(
                metadata: ProjectMetadata,
                environment: ModEnvironment,
                clientDataGenerators: List<String>,
            ): EntryPoints {
                val packageName = metadata.packageName
                val baseClassName = "Mod"
                if (environment == ModEnvironment.SERVER_ONLY) {
                    return EntryPoints(
                        serverEntryPoints = listOf(EntryPoint(packageName.appendPackageName(baseClassName + "Server")))
                    )
                }
                val clientDataGeneratorEntryPoints = clientDataGenerators.map { EntryPoint(it) }.ifEmpty { null }
                val clientEntryPoints = listOf(EntryPoint(packageName.appendPackageName(baseClassName + "Client")))
                if (environment == ModEnvironment.CLIENT_ONLY) {
                    return EntryPoints(
                        clientEntryPoints = clientEntryPoints,
                        dataGenerators = clientDataGeneratorEntryPoints,
                    )
                }
                return EntryPoints(
                    clientEntryPoints = clientEntryPoints,
                    mainEntryPoints = listOf(EntryPoint(packageName.appendPackageName(baseClassName))),
                    dataGenerators = clientDataGeneratorEntryPoints,
                )
            }
        }
    }

    @Serializable
    data class EntryPoint(
        val value: String,
        val adapter: String = "kotlin",
    )

    companion object {
        fun of(
            metadata: ProjectMetadata,
            environment: ModEnvironment,
            targetVersion: String,
            loaderVersion: String,
            isApiRequired: Boolean,
            clientDataGenerators: List<String>
        ): FabricModConfig =
            FabricModConfig(
                schemaVersion = 1,
                id = metadata.slug,
                version = metadata.version,
                name = metadata.name,
                description = metadata.description,
                authors = listOf(Developer.name),
                license = metadata.license.id,
                icon = "assets/${metadata.slug}/icon.png",
                environment = environment.fabricConfigValue,
                mixins = listOf(
                    
                ),
                accessWidener = fileName(metadata.slug, "accesswidener"),
                links = Links.of(metadata.owner.getRepositoryUrl(metadata.slug)),
                entryPoints = EntryPoints.of(metadata, environment, clientDataGenerators),
                dependencies = Dependencies.of(metadata.jvmTarget.toInt(), targetVersion, loaderVersion, isApiRequired)
            )
    }
}
