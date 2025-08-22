package io.github.diskria.organizations.minecraft

enum class ModLoader(val birthdayVersion: String) {

    FABRIC("1.14.4");

    fun getTitle(): String =
        name.lowercase()
}
