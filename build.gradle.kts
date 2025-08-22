import io.github.diskria.Developer
import io.github.diskria.gradle.organizations.extensions.buildGithubUrl
import io.github.diskria.utils.kotlin.delegates.toAutoNamedProperty
import io.github.diskria.utils.kotlin.extensions.setCase
import io.github.diskria.utils.kotlin.words.KebabCase
import io.github.diskria.utils.kotlin.words.ScreamingSnakeCase
import io.github.diskria.utils.kotlin.words.SpaceCase

plugins {
    signing
    `kotlin-dsl`
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.build.config)
    alias(libs.plugins.portal.publish)
}

val gitName: String = "git"

private val plugin = object {
    val projectName: String = rootProject.name
    val projectDescription: String by project
    val projectVersion: String by project
    val slug: String = name.setCase(SpaceCase, KebabCase).lowercase()
    val id: String = "${Developer.NAMESPACE}.${slug}"
}

private val repo = object {
    val url: String = buildGithubUrl(null, plugin.slug)
}

val javaVersion: Int = libs.versions.java.get().toInt()

group = Developer.NAMESPACE
version = plugin.projectVersion

dependencies {
    implementation(libs.kotlin.serialization)
    implementation(libs.kotlin.utils)

    implementation(libs.ktor)
}

java {
    JavaVersion.toVersion(javaVersion).let {
        sourceCompatibility = it
        targetCompatibility = it
    }
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(javaVersion)
}

buildConfig {
    packageName(plugin.id)

    val developerName by Developer.NAME.toAutoNamedProperty(ScreamingSnakeCase)
    val developerEmail by Developer.EMAIL.toAutoNamedProperty(ScreamingSnakeCase)
    val developerNamespace by Developer.NAMESPACE.toAutoNamedProperty(ScreamingSnakeCase)

    listOf(developerName, developerEmail, developerNamespace).forEach {
        buildConfigField(it.name, it.value)
    }
}

gradlePlugin {
    repo.url.let {
        website.set(it)
        vcsUrl.set("$it.$gitName")
    }

    plugins {
        plugin.let {
            create(it.projectName) {
                id = it.id
                implementationClass = "${it.id}.OrganizationsPlugin"

                displayName = it.projectName
                description = it.projectDescription
            }
        }
    }
}
