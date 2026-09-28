plugins {
    alias(libs.plugins.investimento.android.library)
    alias(libs.plugins.investimento.hilt)
    alias(libs.plugins.investimento.room)
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.investimentoeasy.core.database"
    // Esquemas exportados viram assets dos testes de migração.
    sourceSets.getByName("test").assets.srcDir("$projectDir/schemas")
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    testImplementation(projects.core.testing)
}
