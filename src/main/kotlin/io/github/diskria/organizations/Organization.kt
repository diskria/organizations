package io.github.diskria.organizations

import io.github.diskria.utils.kotlin.extensions.common.buildUrl
import io.ktor.http.*

sealed class Organization(val name: String) {
    fun getRepositoryUrl(projectName: String): String =
        buildUrl("github.com") {
            path(name, projectName)
        }

    fun getGithubPath(projectName: String): String =
        "github.com/$name/$projectName"
}

object DarlingramOrganization : Organization("Darlingram")
object AchieveToDoOrganization : Organization("AchieveToDo")
object ForkyOrganization : Organization("ForkyLab")

open class AreaOrganization(name: String) : Organization("${BuildConfig.DEVELOPER_NAME}-$name")

object AndroidOrganization : AreaOrganization("android")
object MinecraftOrganization : AreaOrganization("mc")
object LibrariesOrganization : AreaOrganization("libs")
