package io.github.diskria.organizations.minecraft

enum class ModLoader {

    FABRIC,
    QUILT,
    NEOFORGE;

    fun getTitle(): String =
        name.lowercase()
}
