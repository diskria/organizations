package io.github.diskria.organizations.metadata

import io.github.diskria.organizations.common.GithubConstants
import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.common.buildEmail
import io.github.diskria.utils.kotlin.extensions.common.modifyIf
import io.github.diskria.utils.kotlin.extensions.removePrefix
import io.ktor.http.*

sealed class Owner(val name: String) {

    open val namespace: String = "io.${GithubConstants.GITHUB_NAME}.${name.lowercase()}"

    abstract val email: String

    fun getRepositoryMavenUrl(slug: String): String =
        buildRepositoryUrl(slug, isMaven = true, isVCS = false).toString()

    fun getRepositoryUrl(slug: String, isVCS: Boolean = false): String =
        buildRepositoryUrl(slug, isMaven = false, isVCS).toString()

    fun getRepositoryPath(slug: String, isVCS: Boolean = false): String =
        buildRepositoryUrl(slug, isMaven = false, isVCS = isVCS).encodedPath.removePrefix(Constants.Char.SLASH)

    private fun buildRepositoryUrl(slug: String, isMaven: Boolean, isVCS: Boolean): Url =
        URLBuilder().apply {
            protocol = URLProtocol.HTTPS
            host = "${GithubConstants.GITHUB_NAME}.com".modifyIf(isMaven) { "maven.pkg.$it" }
            path(name, slug.modifyIf(isVCS) { "$it.${GithubConstants.GIT_NAME}" })
        }.build()
}

sealed class Profile(val username: String) : Owner(username)

object Developer : Profile("diskria") {
    override val email: String = buildEmail(username, "proton.me")
}

sealed class Organization(name: String) : Owner(name)

open class DeveloperOrganization(
    val profile: Profile,
    name: String,
) : Organization(profile.username + Constants.Char.HYPHEN + name) {
    override val namespace: String = profile.namespace
    override val email: String = profile.email
}

object MinecraftOrganization : DeveloperOrganization(Developer, "mc")
object AndroidOrganization : DeveloperOrganization(Developer, "android")
object LibrariesOrganization : DeveloperOrganization(Developer, "libs")

open class BrandOrganization(name: String) : Owner(name) {
    override val email: String = Developer.email
}

object ForkyLabOrganization : BrandOrganization("ForkyLab")
object DarlingramOrganization : BrandOrganization("Darlingram")
object AchieveToDoOrganization : BrandOrganization("AchieveToDo")
