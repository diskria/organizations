package io.github.diskria.organizations.metadata

import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.appendPrefix
import io.github.diskria.utils.kotlin.extensions.appendSuffix
import io.github.diskria.utils.kotlin.extensions.common.buildEmail
import io.github.diskria.utils.kotlin.extensions.common.modifyIf
import io.github.diskria.utils.kotlin.extensions.removePrefix
import io.ktor.http.*

sealed class Owner(val name: String) {

    open val namespace: String = "io.${SoftwareForgeType.GITHUB.title}.${name.lowercase()}"

    abstract val email: String

    fun getRepositoryUrl(slug: String, isVcsUrl: Boolean = false): String =
        buildRepositoryUrl(slug, isVcsUrl, isMaven = false).toString()

    fun getRepositoryPath(slug: String, isVcsUrl: Boolean = false): String =
        buildRepositoryUrl(slug, isVcsUrl = isVcsUrl, isMaven = false).encodedPath.removePrefix(Constants.Char.SLASH)

    fun getRepositoryMavenUrl(slug: String): String =
        buildRepositoryUrl(slug, isVcsUrl = false, isMaven = true).toString()

    private fun buildRepositoryUrl(slug: String, isVcsUrl: Boolean, isMaven: Boolean): Url =
        URLBuilder().apply {
            protocol = URLProtocol.HTTPS
            host = SoftwareForgeType.GITHUB.hostname.modifyIf(isMaven) { it.appendPrefix("maven.pkg.") }
            path(name, slug.modifyIf(isVcsUrl) { it.appendSuffix(".${ScmType.GIT.providerName}") })
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
