package io.github.diskria.organizations.minecraft

enum class ModSide(val title: String, val runConfigurationName: String) {
    CLIENT("client", "Minecraft Client"),
    SERVER("server", "Minecraft Server"),
}
