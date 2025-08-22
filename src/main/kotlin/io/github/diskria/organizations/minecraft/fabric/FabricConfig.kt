package io.github.diskria.organizations.minecraft.fabric

import kotlinx.serialization.Serializable

@Serializable
data class FabricConfig(
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
    val entrypoints: EntryPoints,
    val mixins: List<String>,
    val depends: FabricModDependencies,
    val accessWidener: String,
)
