plugins {
    alias(libs.plugins.investimento.android.library)
    alias(libs.plugins.investimento.android.compose)
}

android {
    namespace = "com.investimentoeasy.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.domain)
}
