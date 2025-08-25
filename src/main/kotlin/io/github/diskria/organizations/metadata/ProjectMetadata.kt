package io.github.diskria.organizations.metadata

import io.github.diskria.organizations.licenses.License

data class ProjectMetadata(
    val owner: Owner,
    val type: ProjectType,
    val license: License,

    val name: String,
    val description: String,
    val version: String,
    val slug: String,
    val packageName: String,

    val javaVersion: Int,

    val scm: ScmType = ScmType.GIT,
)
