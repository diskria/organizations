package io.github.diskria.organizations.metadata

import io.github.diskria.utils.kotlin.Constants

enum class SoftwareForgeType(val title: String, val hostname: String, val scmType: ScmType) {

    GITHUB("github", "github.com", ScmType.GIT);

    fun getSshAuthority(): String =
        scmType.providerName + Constants.Char.AT + hostname
}
