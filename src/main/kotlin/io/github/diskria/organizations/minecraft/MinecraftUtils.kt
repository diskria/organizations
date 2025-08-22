package io.github.diskria.organizations.minecraft

import io.github.diskria.utils.kotlin.delegates.toAutoNamedProperty
import io.github.diskria.utils.kotlin.extensions.common.failWithDetails
import io.github.diskria.utils.kotlin.extensions.toSemver

object MinecraftUtils {

    private val javaVersions: Map<String, Int> = mapOf(
        "1.20.5" to 21,
        "1.18" to 17,
        "1.17" to 16,
        ModLoader.FABRIC.birthdayVersion to 8,
    )

    fun getRuntimeJavaVersion(targetVersion: String): Int {
        val targetSemver = targetVersion.toSemver()
        return javaVersions
            .mapKeys { it.key.toSemver() }
            .filterKeys { it <= targetSemver }
            .maxByOrNull { it.key }
            ?.value
            ?: failWithDetails("Too old minecraft version") {
                val minecraftVersion by targetVersion.toAutoNamedProperty()
                listOf(minecraftVersion)
            }
    }
}
