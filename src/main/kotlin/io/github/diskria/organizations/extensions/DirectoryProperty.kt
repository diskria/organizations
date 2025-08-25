package io.github.diskria.organizations.extensions

import org.gradle.api.file.DirectoryProperty
import java.io.File

fun DirectoryProperty.resolve(path: String): File =
    dir(path).get().asFile
