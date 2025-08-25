package io.github.diskria.organizations.metadata

import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.generics.joinToString

enum class ScmType(val providerName: String) {

    GIT("git");

    fun buildUri(vararg parts: String): String =
        listOf("scm", providerName, *parts).joinToString(Constants.Char.COLON)
}
