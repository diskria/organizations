package io.github.diskria.organizations.minecraft.fabric

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FabricModDependencies(
    @SerialName("java")
    val javaVersion: String,

    @SerialName("fabricloader")
    val loaderVersion: String,

    @SerialName("fabric-api")
    val apiVersion: String = "*",
)
