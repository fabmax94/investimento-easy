plugins {
    alias(libs.plugins.investimento.jvm.library)
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    api(projects.core.domain)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(projects.core.testing)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    systemProperty("fontesReais", providers.gradleProperty("fontesReais").isPresent.toString())
    // Chamadas reais nunca vêm do cache do Gradle.
    if (providers.gradleProperty("fontesReais").isPresent) outputs.upToDateWhen { false }
}
