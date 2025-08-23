package io.github.diskria

import io.github.diskria.utils.kotlin.extensions.common.buildEmail

object Developer {
    const val NAME = "diskria"
    const val NAMESPACE = "io.github.$NAME"

    val EMAIL = buildEmail(NAME, "proton.me")
}
