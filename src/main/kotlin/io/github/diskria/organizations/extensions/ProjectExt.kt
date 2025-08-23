package io.github.diskria.organizations.extensions

import io.github.diskria.organizations.GradlePluginMetadata
import io.github.diskria.organizations.LibraryMetadata
import io.github.diskria.organizations.Owner
import io.github.diskria.organizations.ProjectMetadata
import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.common.failWithUnsupportedType
import io.github.diskria.utils.kotlin.extensions.setCase
import io.github.diskria.utils.kotlin.words.KebabCase
import io.github.diskria.utils.kotlin.words.SpaceCase
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import java.nio.charset.StandardCharsets

inline fun <reified T : ProjectMetadata> Project.buildMetadata(owner: Owner): T {
    val projectName: String by rootProject
    val projectDescription: String by rootProject
    val projectVersion: String by rootProject

    val slug = projectName.setCase(SpaceCase, KebabCase).lowercase()

    return when (val clazz = T::class) {
        GradlePluginMetadata::class -> GradlePluginMetadata(
            owner = owner,
            name = projectName,
            description = projectDescription,
            version = projectVersion,
            slug = slug,
        )

        LibraryMetadata::class -> LibraryMetadata(
            owner = owner,
            name = projectName,
            description = projectDescription,
            version = projectVersion,
            slug = slug,
        )

        else -> failWithUnsupportedType(clazz)
    } as T
}

fun Project.setJavaCompatibilityVersion(version: Int) {
    JavaVersion.toVersion(version).let { javaVersion ->
        with(extensions.getByType<JavaPluginExtension>()) {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
    }
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(version)
    }
}

fun Project.applyJavaUTF8Encoding() {
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = StandardCharsets.UTF_8.name()
    }
}

fun Project.includeLicenseInJar(metadata: ProjectMetadata) {
    tasks.named<Jar>("jar") {
        from("LICENSE") { rename { it + Constants.Char.UNDERSCORE + metadata.name } }
    }
}
