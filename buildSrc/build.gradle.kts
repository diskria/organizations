plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.serialization)
    implementation(libs.ktor.http)
    implementation(libs.kotlin.utils)

    implementation(libs.kotlin.gradle.plugin)
    compileOnly(libs.modrinth.minotaur)

    constraints {
        // Override vulnerable transitive dependency (Okio < 3.4.0, CVE-2023-3635)
        // Minotaur → Modrinth4J → OkHttp/Okio
        implementation(libs.okio)
    }
}

sourceSets.main {
    kotlin.srcDirs("../src")
}
