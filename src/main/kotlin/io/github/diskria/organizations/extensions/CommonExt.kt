package io.github.diskria.organizations.extensions

import org.gradle.api.GradleException

fun gradleError(message: String): Nothing =
    throw GradleException(message)
