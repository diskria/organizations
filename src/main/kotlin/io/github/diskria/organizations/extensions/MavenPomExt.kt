package io.github.diskria.organizations.extensions

import io.github.diskria.organizations.licenses.License
import io.github.diskria.organizations.metadata.Owner
import org.gradle.api.publish.maven.MavenPom

fun MavenPom.configureLicense(license: License) {
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
