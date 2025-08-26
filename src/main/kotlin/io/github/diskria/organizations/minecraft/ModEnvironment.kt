package io.github.diskria.organizations.minecraft

enum class ModEnvironment(val fabricConfigValue: String, val fabricEntryPointValue: String) {
    CLIENT_ONLY("client", "client"),
    SERVER_ONLY("server", "server"),
    CLIENT_SERVER("*", "main"),
}
