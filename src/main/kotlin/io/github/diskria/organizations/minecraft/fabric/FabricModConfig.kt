package io.github.diskria.organizations.minecraft.fabric

import io.github.diskria.organizations.extensions.toInt
import io.github.diskria.organizations.metadata.DiskriaDeveloper
import io.github.diskria.organizations.metadata.ProjectMetadata
import io.github.diskria.organizations.minecraft.ModEnvironmentType
import io.github.diskria.organizations.minecraft.ModEnvironmentType.*
import io.github.diskria.organizations.minecraft.ModSide
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
            fun of(
                metadata: ProjectMetadata,
                environment: ModEnvironmentType,
                dataGenerators: List<String>,
            ): EntryPoints {
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
    sealed class MixinsConfigEntry(val fileName: String) {

        class MainConfigEntry(modId: String) : MixinsConfigEntry("$modId.mixins.json")
        class SideConfigEntry(modId: String, val side: ModSide) : MixinsConfigEntry("$modId.${side.title}.mixins.json")

        companion object {
            fun of(metadata: ProjectMetadata, environment: ModEnvironmentType): List<MixinsConfigEntry> {
                val modId = metadata.slug
                val mainConfig = MainConfigEntry(modId)
                val clientSideConfig = SideConfigEntry(modId, ModSide.CLIENT)
                val serverSideConfig = SideConfigEntry(modId, ModSide.SERVER)
                return when (environment) {
                    CLIENT_SERVER -> listOf(mainConfig, clientSideConfig, serverSideConfig)
                    CLIENT_ONLY -> listOf(clientSideConfig)
                    SERVER_ONLY -> listOf(serverSideConfig)
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
                    is MixinsConfigEntry.MainConfigEntry -> JsonPrimitive(value.fileName)
                    is MixinsConfigEntry.SideConfigEntry -> buildJsonObject {
                        put("config", value.fileName)
                        put("environment", value.side.title)
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
            modEnvironmentType: ModEnvironmentType,
            targetVersion: String,
            loaderVersion: String,
            isApiRequired: Boolean,
            dataGenerators: List<String>,
        ): FabricModConfig =
            FabricModConfig(
                schemaVersion = 1,
                id = metadata.slug,
                version = metadata.version,
                name = metadata.name,
                description = metadata.description,
                authors = listOf(DiskriaDeveloper.name),
                license = metadata.license.id,
                icon = "assets/${metadata.slug}/icon.png",
                environment = modEnvironmentType.fabricConfigEnvironment,
                accessWidener = fileName(metadata.slug, "accesswidener"),
                mixins = MixinsConfigEntry.of(metadata, modEnvironmentType),
                links = Links.of(
                    metadata.owner.getRepositoryUrl(metadata.slug)
                ),
                entryPoints = EntryPoints.of(
                    metadata,
                    modEnvironmentType,
                    dataGenerators,
                ),
                dependencies = Dependencies.of(
                    metadata.jvmTarget.toInt(),
                    targetVersion,
                    loaderVersion,
                    isApiRequired,
                )
            )
    }
}
