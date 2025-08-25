package io.github.diskria.organizations.metadata

import io.github.diskria.organizations.licenses.License

data class ProjectMetadata(
    val type: ProjectType,
    val owner: Owner,
    val license: License,
    val javaVersion: Int,
    val name: String,
    val description: String,
    val version: String,
    val slug: String,
    val url: String,
)
