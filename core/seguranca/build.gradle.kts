plugins {
    alias(libs.plugins.investimento.android.library)
    alias(libs.plugins.investimento.hilt)
}

android {
    namespace = "com.investimentoeasy.core.seguranca"
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
