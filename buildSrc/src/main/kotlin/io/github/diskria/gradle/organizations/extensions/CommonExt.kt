package io.github.diskria.gradle.organizations.extensions

import io.github.diskria.Developer
import io.github.diskria.utils.kotlin.extensions.common.buildUrl
import io.ktor.http.*

fun buildGithubUrl(organization: String?, repositoryName: String): String =
    buildUrl("github.com") {
        path(organization ?: Developer.NAME, repositoryName)
    }
