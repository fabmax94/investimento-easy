plugins {
    alias(libs.plugins.investimento.android.library)
    alias(libs.plugins.investimento.hilt)
    alias(libs.plugins.investimento.room)
}

android {
    namespace = "com.investimentoeasy.core.database"
}

dependencies {
    implementation(projects.core.domain)
    testImplementation(projects.core.testing)
}
