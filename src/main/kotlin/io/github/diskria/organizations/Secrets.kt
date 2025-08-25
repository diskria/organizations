package io.github.diskria.organizations

object Secrets {
    val githubPackagesToken: String? = System.getenv("GITHUB_PACKAGES_TOKEN")

    val gpgKey: String? = System.getenv("GPG_KEY")
    val gpgPassphrase: String? = System.getenv("GPG_PASSPHRASE")
}
