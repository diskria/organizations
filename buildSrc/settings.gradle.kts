fun RepositoryHandler.attachCommonRepositories() {
    mavenCentral()
    maven("https://plugins.gradle.org/m2")
}

fun RepositoryHandler.attachPluginRepositories() {
    gradlePluginPortal()
}

@Suppress("UnstableApiUsage")
fun setupRepositories() {
    dependencyResolutionManagement.repositories {
        attachCommonRepositories()
    }

    pluginManagement.repositories {
        attachCommonRepositories()
        attachPluginRepositories()
    }
}

fun setupVersionCatalogs() {
    dependencyResolutionManagement
        .versionCatalogs
        .create("libs")
        .from(files("../gradle/libs.versions.toml"))
}

setupRepositories()
setupVersionCatalogs()
