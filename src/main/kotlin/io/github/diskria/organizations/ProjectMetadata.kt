package io.github.diskria.organizations

data class ProjectMetadata(
    val type: ProjectType,
    val javaVersion: Int,
    val owner: Owner,
    val name: String,
    val description: String,
    val version: String,
    val slug: String,
    val url: String,
)
