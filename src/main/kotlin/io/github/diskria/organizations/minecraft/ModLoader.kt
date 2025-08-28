package io.github.diskria.organizations.minecraft

enum class ModLoader {

    FABRIC,
    QUILT,
    FORGE,
    NEOFORGE;

    fun getTitle(): String =
        name.lowercase()
}
