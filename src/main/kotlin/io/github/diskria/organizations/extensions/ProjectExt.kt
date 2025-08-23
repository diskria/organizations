package io.github.diskria.organizations.extensions

import io.github.diskria.organizations.*
import io.github.diskria.organizations.licenses.LicenseType
import io.github.diskria.organizations.publishing.PublishingTarget
import io.github.diskria.organizations.publishing.PublishingTarget.*
import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.appendPackageName
import io.github.diskria.utils.kotlin.extensions.capitalizeFirstChar
import io.github.diskria.utils.kotlin.extensions.setCase
import io.github.diskria.utils.kotlin.words.KebabCase
import io.github.diskria.utils.kotlin.words.SpaceCase
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import java.nio.charset.StandardCharsets

fun Project.javaExtension(): JavaPluginExtension =
    extensions.getByType<JavaPluginExtension>()

fun Project.gradlePluginDevelopmentExtension(): GradlePluginDevelopmentExtension =
    extensions.getByType<GradlePluginDevelopmentExtension>()

fun Project.publishingExtension(): PublishingExtension =
    extensions.getByType<PublishingExtension>()

fun Project.basePluginExtension(): BasePluginExtension =
    extensions.getByType<BasePluginExtension>()

fun Project.signingExtension(): SigningExtension =
    extensions.getByType<SigningExtension>()

fun Project.buildProjectMetadata(owner: Owner): ProjectMetadata {
    val projectName: String by rootProject
    val projectDescription: String by rootProject
    val projectVersion: String by rootProject
    val slug = projectName.setCase(SpaceCase, KebabCase).lowercase()
    return ProjectMetadata(
        owner = owner,
        name = projectName,
        description = projectDescription,
        version = projectVersion,
        slug = slug,
        url = owner.getRepositoryUrl(slug),
    )
}

fun Project.configureJava(version: Int) {
    with(javaExtension()) {
        JavaVersion.toVersion(version).let { javaVersion ->
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
        withSourcesJar()
        withJavadocJar()
    }
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(version)
        options.encoding = StandardCharsets.UTF_8.name()
    }
    kotlinExtension.jvmToolchain(version)
}

fun Project.configureProject(metadata: ProjectMetadata, publishingTarget: PublishingTarget? = null) {
    group = metadata.owner.namespace
    version = metadata.version

    with(basePluginExtension()) {
        archivesName = metadata.slug
    }
    tasks.named<Jar>("jar") {
        from("LICENSE") {
            rename { it + Constants.Char.UNDERSCORE + metadata.name }
        }
    }
    if (publishingTarget != null) {
        configurePublishing(metadata, publishingTarget)
    }
}

fun Project.configureGradlePlugin(owner: Owner = Developer, publishingTarget: PublishingTarget? = null) {
    val metadata = buildProjectMetadata(owner)
    configureProject(metadata, publishingTarget)

    with(gradlePluginDevelopmentExtension()) {
        val pId = owner.namespace.appendPackageName(metadata.slug)
        plugins.create(pId) {
            id = pId
            implementationClass = id.appendPackageName(metadata.slug.capitalizeFirstChar(true) + "GradlePlugin")
        }
    }
}

fun Project.configureLibrary() {
    configureProject(buildProjectMetadata(LibrariesOrganization), MAVEN_CENTRAL)
}

fun Project.configurePublishing(metadata: ProjectMetadata, target: PublishingTarget) {
    when (target) {
        GITHUB_PACKAGES -> with(publishingExtension()) {
            publications.withType<MavenPublication> {
                artifactId = metadata.slug
            }
            repositories.maven {
                url = uri(metadata.owner.getRepositoryUrl(metadata.slug, isMaven = true))
                credentials {
                    username = metadata.owner.name
                    password = Secrets.githubPackagesToken
                }
            }
        }

        MAVEN_CENTRAL -> {
            val publishing = publishingExtension()
            publishing.repositories.maven {
                url = layout.buildDirectory.dir("staging-repo").get().asFile.toURI()
            }
            val publication = with(publishing) {
                publications.create<MavenPublication>(metadata.slug) {
                    artifactId = metadata.slug
                    from(components["java"])
                    pom {
                        name.set(metadata.name)
                        description.set(metadata.description)
                        url.set(metadata.url)
                        applyLicense(LicenseType.MIT)
                        applyDeveloper(metadata.owner)
                        scm {
                            url.set(metadata.url)
                            connection.set("scm:git:${metadata.url}.git")
                            developerConnection.set(
                                "scm:git:git@github.com:${metadata.owner.getRepositoryPath(metadata.slug)}.git"
                            )
                        }
                    }
                }
            }
            val gpgKey = System.getenv("GPG_KEY")
            val gpgPassphrase = System.getenv("GPG_PASSPHRASE")
            if (!gpgKey.isNullOrBlank() && !gpgPassphrase.isNullOrBlank()) {
                with(signingExtension()) {
                    useInMemoryPgpKeys(gpgKey, gpgPassphrase)
                    sign(publication)
                }
            }
        }

        GRADLE_PLUGIN_PORTAL -> {

        }
    }
}
