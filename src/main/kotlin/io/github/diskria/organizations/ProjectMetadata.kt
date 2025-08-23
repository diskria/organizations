package io.github.diskria.organizations

import io.github.diskria.utils.kotlin.extensions.appendPackageName
import io.github.diskria.utils.kotlin.extensions.capitalizeFirstChar

sealed class ProjectMetadata(
    val owner: Owner,
    val name: String,
    val description: String,
    val version: String,
    val slug: String,
)

class GradlePluginMetadata(
    owner: Owner,
    name: String,
    description: String,
    version: String,
    slug: String,
    val id: String = Developer.namespace.appendPackageName(slug),
    val implementationClass: String = id.appendPackageName(slug.capitalizeFirstChar(true) + "GradlePlugin")
) : ProjectMetadata(owner, name, description, version, slug)

class LibraryMetadata(
    owner: Owner,
    name: String,
    description: String,
    version: String,
    slug: String,
) : ProjectMetadata(owner, name, description, version, slug)
