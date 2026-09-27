plugins {
    alias(libs.plugins.investimento.android.library)
    alias(libs.plugins.investimento.hilt)
}

android {
    namespace = "com.investimentoeasy.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
