plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlin.serialization)
    implementation(libs.ktor.http)

    implementation(libs.kotlin.utils)
}

sourceSets.main {
    kotlin.srcDirs("../src")
}
