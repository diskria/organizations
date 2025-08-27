package io.github.diskria.organizations.minecraft

import io.github.diskria.organizations.extensions.toInt
import io.github.diskria.organizations.metadata.Developer
import io.github.diskria.organizations.metadata.ProjectMetadata
import io.github.diskria.organizations.minecraft.EnvironmentScope.*
import io.github.diskria.utils.kotlin.extensions.appendPackageName
import io.github.diskria.utils.kotlin.extensions.common.className
import io.github.diskria.utils.kotlin.extensions.common.fileName
import io.github.diskria.utils.kotlin.extensions.common.unsupportedOperation
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
    val mixins: List<MixinsConfigEntry>,
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
        @SerialName("homepage")
        val siteUrl: String,

        @SerialName("sources")
        val sourceUrl: String,
    ) {
        companion object {
            fun of(sourceUrl: String, siteUrl: String = sourceUrl): Links =
                Links(
                    siteUrl = siteUrl,
                    sourceUrl = sourceUrl,
                )
        }
    }

    @Serializable
    class Dependencies(
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
                    jvmDependency = VersionCondition.min(javaVersion.toString()),
                    minecraftDependency = VersionCondition.min(minecraftVersion),
                    loaderDependency = VersionCondition.min(loaderVersion),
                    apiDependency = if (isApiRequired) VersionCondition.ANY_VERSION else null,
                )
        }
    }

    @Serializable
    class EntryPoints(
        @SerialName("main")
        val mainEntryPoints: List<EntryPoint>? = null,

        @SerialName("client")
        val clientEntryPoints: List<EntryPoint>? = null,

        @SerialName("server")
        val serverEntryPoints: List<EntryPoint>? = null,

        @SerialName("fabric-datagen")
        val dataGenerators: List<EntryPoint>? = null,
    ) {
        companion object {
            fun of(metadata: ProjectMetadata, environment: EnvironmentScope, dataGenerators: List<String>): EntryPoints {
                val packageName = metadata.packageName
                val classNameBase = "Mod"
                val dataGeneratorEntryPoints = dataGenerators.map { EntryPoint.of(it) }.ifEmpty { null }

                val mainEntryPoints = entryPoints(packageName.appendPackageName(classNameBase))
                val clientEntryPoints = entryPoints(packageName.appendPackageName(classNameBase + "Client"))
                val serverEntryPoints = entryPoints(packageName.appendPackageName(classNameBase + "Server"))

                return when (environment) {
                    SERVER_ONLY -> EntryPoints(
                        serverEntryPoints = serverEntryPoints
                    )

                    CLIENT_ONLY -> EntryPoints(
                        clientEntryPoints = clientEntryPoints,
                        dataGenerators = dataGeneratorEntryPoints,
                    )

                    CLIENT_SERVER -> EntryPoints(
                        mainEntryPoints = mainEntryPoints,
                        clientEntryPoints = clientEntryPoints,
                        serverEntryPoints = serverEntryPoints,
                        dataGenerators = dataGeneratorEntryPoints,
                    )
                }
            }

            private fun entryPoints(vararg classPaths: String): List<EntryPoint> =
                classPaths.map { EntryPoint.of(it) }
        }
    }

    @Serializable
    class EntryPoint(
        val adapter: String,

        @SerialName("value")
        val classPath: String,
    ) {
        companion object {
            fun of(classPath: String): EntryPoint =
                EntryPoint(
                    classPath = classPath,
                    adapter = "kotlin",
                )
        }
    }

    @Serializable(with = MixinEntrySerializer::class)
    sealed class MixinsConfigEntry {

        data class MainConfigEntry(val config: String) : MixinsConfigEntry()

        data class EnvironmentConfigEntry(
            val config: String,
            val environment: String
        ) : MixinsConfigEntry()

        companion object {
            fun of(metadata: ProjectMetadata, environment: EnvironmentScope): List<MixinsConfigEntry> {
                val mainConfig = MainConfigEntry("${metadata.slug}.mixins.json")
                val clientConfig = EnvironmentConfigEntry("${metadata.slug}.client.mixins.json", "client")
                val serverConfig = EnvironmentConfigEntry("${metadata.slug}.server.mixins.json", "server")
                return when (environment) {
                    CLIENT_SERVER -> listOf(mainConfig, clientConfig, serverConfig)
                    CLIENT_ONLY -> listOf(clientConfig)
                    SERVER_ONLY -> listOf(serverConfig)
                }
            }
        }
    }

    object MixinEntrySerializer : KSerializer<MixinsConfigEntry> {

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(MixinsConfigEntry::class.className())

        override fun serialize(encoder: Encoder, value: MixinsConfigEntry) {
            encoder as JsonEncoder
            encoder.encodeJsonElement(
                when (value) {
                    is MixinsConfigEntry.MainConfigEntry -> JsonPrimitive(value.config)
                    is MixinsConfigEntry.EnvironmentConfigEntry -> buildJsonObject {
                        put("config", value.config)
                        put("environment", value.environment)
                    }
                }
            )
        }

        override fun deserialize(decoder: Decoder): MixinsConfigEntry = unsupportedOperation()
    }

    private object VersionCondition {
        const val ANY_VERSION: String = "*"

        fun min(version: String): String =
            ">=$version"
    }

    companion object {
        fun of(
            metadata: ProjectMetadata,
            environment: EnvironmentScope,
            targetVersion: String,
            loaderVersion: String,
            isApiRequired: Boolean,
            dataGenerators: List<String>
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
                environment = environment.fabricConfigEnvironment,
                accessWidener = fileName(metadata.slug, "accesswidener"),
                mixins = MixinsConfigEntry.of(metadata, environment),
                links = Links.of(
                    metadata.owner.getRepositoryUrl(metadata.slug)
                ),
                entryPoints = EntryPoints.of(
                    metadata,
                    environment,
                    dataGenerators
                ),
                dependencies = Dependencies.of(
                    metadata.jvmTarget.toInt(),
                    targetVersion,
                    loaderVersion,
                    isApiRequired
                )
            )
    }
}
