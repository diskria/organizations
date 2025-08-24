package io.github.diskria.organizations.extensions

import io.github.diskria.organizations.Owner
import io.github.diskria.organizations.licenses.LicenseType
import org.gradle.api.publish.maven.MavenPom

fun MavenPom.configureLicense(license: LicenseType) {
    licenses {
        license {
            name.set(license.displayName)
            url.set(license.getUrl())
        }
    }
}

fun MavenPom.configureDevelopers(owner: Owner) {
    developers {
        developer {
            owner.name.let {
                id.set(it)
                name.set(it)
            }
            email.set(owner.email)
        }
    }
}
