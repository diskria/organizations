package io.github.diskria.organizations.extensions

import io.github.diskria.organizations.Owner
import io.github.diskria.organizations.licenses.LicenseType
import org.gradle.api.publish.maven.MavenPom

fun MavenPom.applyLicense(licenseType: LicenseType) {
    licenses {
        license {
            name.set(licenseType.displayName)
            url.set(licenseType.getUrl())
        }
    }
}

fun MavenPom.applyDeveloper(owner: Owner) {
    developers {
        developer {
            id.set(owner.name)
            name.set(owner.name)
            email.set(owner.email)
        }
    }
}