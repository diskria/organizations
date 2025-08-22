package io.github.diskria.organizations

object Secrets {
    val githubPackagesToken: String by lazy { System.getenv("GITHUB_PACKAGES_TOKEN") }
}
