plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlin.serialization)
    implementation(libs.kotlin.utils)

    implementation(libs.ktor)
}
