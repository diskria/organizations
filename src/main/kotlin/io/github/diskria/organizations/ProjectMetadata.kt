package io.github.diskria.organizations

data class ProjectMetadata(
    val owner: Owner,
    val name: String,
    val description: String,
    val version: String,
    val slug: String,
    val url: String,
)
