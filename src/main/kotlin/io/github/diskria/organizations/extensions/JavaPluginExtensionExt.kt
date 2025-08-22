package io.github.diskria.organizations.extensions

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByType

fun JavaPluginExtension.setCompatibilityVersion(version: Int) {
    JavaVersion.toVersion(version).let { javaVersion ->
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

fun Project.setJavaCompatibilityVersion(version: Int) {
    extensions.getByType<JavaPluginExtension>().setCompatibilityVersion(version)
}
