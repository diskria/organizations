package io.github.diskria.organizations.minecraft

import io.github.diskria.organizations.metadata.ProjectMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FabricModConfig(
    val schemaVersion: Int,
    val id: String,
    val version: String,
    val name: String,
    val description: String,
    val authors: List<String>,
    val contact: Map<String, String>,
    val license: String,
    val icon: String,
    val environment: String,
    val mixins: List<String>,
    val accessWidener: String,

    @SerialName("entrypoints")
    val entryPoints: EntryPoints,

    @SerialName("depends")
    val dependencies: Dependencies,
) {
    @Serializable
    data class Dependencies(
        @SerialName("java")
        val javaVersion: String,

        @SerialName("minecraft")
        val minecraftVersion: String,

        @SerialName("fabricloader")
        val loaderVersion: String,

        @SerialName("fabric-api")
        val apiVersion: String = "*",
    )

    @Serializable
    data class EntryPoints(
        @SerialName("client")
        val clientEntryPoint: List<String> = emptyList(),

        @SerialName("main")
        val mainEntryPoint: List<String>? = null,

        @SerialName("fabric-datagen")
        val dataGenerators: List<String> = emptyList(),
    )

    companion object {
        fun of(metadata: ProjectMetadata): FabricModConfig {
            TODO()
        }
    }
}
