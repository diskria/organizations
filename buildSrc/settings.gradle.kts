fun RepositoryHandler.mavenGradlePluginPortal() {
    maven("https://plugins.gradle.org/m2")
}

fun RepositoryHandler.commonRepositories() {
    mavenCentral()
    mavenGradlePluginPortal()
}

fun RepositoryHandler.pluginRepositories() {
    gradlePluginPortal()
}

fun setupRepositories() {
    dependencyResolutionManagement.repositories {
        commonRepositories()
    }

    pluginManagement.repositories {
        commonRepositories()
        pluginRepositories()
    }
}

setupRepositories()

fun setupVersionCatalogs() {
    dependencyResolutionManagement
        .versionCatalogs
        .create("libs")
        .from(files("../gradle/libs.versions.toml"))
}

setupVersionCatalogs()
