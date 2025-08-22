fun RepositoryHandler.attachCommonRepositories() {
    mavenCentral()
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
