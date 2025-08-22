package io.github.diskria.organizations.minecraft.fabric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EntryPoints(
    @SerialName("fabric-datagen")
    val dataGenerators: List<String> = emptyList(),

    @SerialName("client")
    val clientEntryPoint: List<String> = emptyList(),

    @SerialName("main")
    val mainEntryPoint: List<String>? = null,
)
