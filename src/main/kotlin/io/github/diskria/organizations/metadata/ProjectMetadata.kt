package io.github.diskria.organizations.metadata

import io.github.diskria.organizations.licenses.License
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

data class ProjectMetadata(
    val owner: Owner,
    val type: ProjectType,
    val license: License,

    val name: String,
    val description: String,
    val version: String,
    val slug: String,
    val packageName: String,

    val jdkVersion: Int,
    val jvmTarget: JvmTarget,

    val scm: ScmType = ScmType.GIT,
)
