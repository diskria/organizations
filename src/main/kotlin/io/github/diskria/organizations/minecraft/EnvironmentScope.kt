package io.github.diskria.organizations.minecraft

enum class EnvironmentScope(
    val fabricConfigEnvironment: String,
    val sourceSetName: String,
    val environments: List<EnvironmentType>,
) {
    CLIENT_SERVER("*", "main", listOf(EnvironmentType.CLIENT, EnvironmentType.SERVER)),
    CLIENT_ONLY("client", "client", listOf(EnvironmentType.CLIENT)),
    SERVER_ONLY("server", "server", listOf(EnvironmentType.SERVER)),
}
