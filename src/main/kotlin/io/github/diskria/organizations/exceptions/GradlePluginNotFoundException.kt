package io.github.diskria.organizations.exceptions

import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.wrap
import org.gradle.api.GradleException

class GradlePluginNotFoundException(
    pluginId: String
) : GradleException(
    "Plugin ${pluginId.wrap(Constants.Char.SINGLE_QUOTE)} required but not applied."
)
