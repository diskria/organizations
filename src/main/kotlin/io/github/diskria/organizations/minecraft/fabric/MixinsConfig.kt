package io.github.diskria.organizations.minecraft.fabric

import io.github.diskria.organizations.extensions.toInt
import io.github.diskria.organizations.metadata.ProjectMetadata
import io.github.diskria.utils.kotlin.extensions.appendPackageName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MixinsConfig(
    val required: Boolean,
    val compatibilityLevel: String,
    val injectors: Injectors,
    val overwrites: Overwrites,
    val client: List<String>,

    @SerialName("package")
    val packageName: String,
) {
    @Serializable
    data class Injectors(
        val defaultRequire: Int,
    )

    @Serializable
    data class Overwrites(
        val requireAnnotations: Boolean,
    )

    companion object {
        fun of(metadata: ProjectMetadata): MixinsConfig =
            MixinsConfig(
                required = true,
                packageName = metadata.packageName.appendPackageName("mixins.client"),
                compatibilityLevel = "JAVA_${metadata.jvmTarget.toInt()}",
                injectors = Injectors(1),
                overwrites = Overwrites(true),
                client = listOf(),
            )
    }
}
