package io.github.diskria.organizations

import io.github.diskria.utils.kotlin.extensions.common.buildEmail
import io.github.diskria.utils.kotlin.extensions.common.buildUrl
import io.github.diskria.utils.kotlin.extensions.common.modifyIf
import io.ktor.http.*

sealed class Owner(val name: String) {

    open val namespace: String = "io.github.${name.lowercase()}"

    fun getRepositoryUrl(slug: String, isMaven: Boolean = false): String =
        buildUrl("github.com".modifyIf(isMaven) { "maven.pkg.$it" }) {
            path(name, slug)
        }
}

sealed class Profile(val username: String) : Owner(username) {
    abstract val email: String
}

object Developer : Profile("diskria") {
    override val email: String = buildEmail(username, "proton.me")
}

sealed class Organization(name: String) : Owner(name)

open class AreaOrganization(name: String) : Organization("${Developer.username}-$name") {
    override val namespace: String = Developer.namespace
}

object MinecraftOrganization : AreaOrganization("mc")
object AndroidOrganization : AreaOrganization("android")
object LibrariesOrganization : AreaOrganization("libs")

open class BrandOrganization(name: String) : Owner(name)

object ForkyLabOrganization : BrandOrganization("ForkyLab")
object DarlingramOrganization : BrandOrganization("Darlingram")
object AchieveToDoOrganization : BrandOrganization("AchieveToDo")
