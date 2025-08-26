plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.plugin)
    compileOnly(libs.build.config.plugin)
    compileOnly(libs.modrinth.minotaur.plugin)
    compileOnly(libs.fabric.loom.plugin)

    implementation(libs.ktor.http)
    implementation(libs.kotlin.utils)
    implementation(libs.kotlin.serialization)

    constraints {
        // Override vulnerable transitive dependency (Okio < 3.4.0, CVE-2023-3635)
        // com.modrinth.minotaur → Modrinth4J → Okio
        implementation(libs.okio)
    }
}

sourceSets.main {
    kotlin {
        srcDirs("../src", "../gradle/settings")
    }
}
