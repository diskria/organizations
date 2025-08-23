package io.github.diskria.organizations

import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.common.buildEmail
import io.github.diskria.utils.kotlin.extensions.common.buildUrl
import io.github.diskria.utils.kotlin.extensions.common.modifyIf
import io.ktor.http.*

sealed class Owner(val name: String) {

    open val namespace: String = "io.github.${name.lowercase()}"

    abstract val email: String

    fun getRepositoryUrl(slug: String, isMaven: Boolean = false): String =
        buildUrl("github.com".modifyIf(isMaven) { "maven.pkg.$it" }) {
            path(name, slug)
        }

    fun getRepositoryPath(slug: String): String =
        URLBuilder().apply {
            this.host = "github.com"
            path(name, slug)
        }.buildString()
}

enum class GithubHost(val hostname: String) {
    MAIN("github.com"),
    PACKAGES("maven.pkg.github.com")
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
