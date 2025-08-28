package io.github.diskria.organizations.minecraft

enum class ModEnvironmentType(
    val fabricConfigEnvironment: String,
    val sourceSets: List<SourceSet>,
    val sides: List<ModSide>,
) {
    CLIENT_SERVER(
        "*",
        listOf(SourceSet.MAIN, SourceSet.CLIENT, SourceSet.SERVER),
        listOf(ModSide.CLIENT, ModSide.SERVER)
    ),
    CLIENT_ONLY(
        "client",
        listOf(SourceSet.CLIENT),
        listOf(ModSide.CLIENT)
    ),
    SERVER_ONLY(
        "server",
        listOf(SourceSet.SERVER),
        listOf(ModSide.SERVER)
    );

    fun getMainSourceSet(): SourceSet =
        sourceSets.first()
}
