plugins {
    alias(libs.plugins.investimento.jvm.library)
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    api(projects.core.domain)
    implementation(libs.anthropic.java)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(projects.core.testing)
    testImplementation(libs.okhttp.mockwebserver)
}
