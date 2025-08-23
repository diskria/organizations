plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization)
    implementation(libs.ktor.http)
    implementation(libs.kotlin.utils)
}

sourceSets.main {
    kotlin.srcDirs("../src")
}
