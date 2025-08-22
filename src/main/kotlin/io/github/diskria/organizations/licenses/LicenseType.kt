package io.github.diskria.organizations.licenses

import io.github.diskria.utils.kotlin.extensions.common.buildUrl
import io.ktor.http.*

enum class LicenseType(val id: String, val displayName: String) {

    MIT(
        "MIT",
        "MIT License"
    ),
    APACHE2(
        "Apache-2.0",
        "Apache License, Version 2.0"
    );

    fun getUrl(): String =
        buildUrl("opensource.org") {
            path("licenses", id)
        }
}
