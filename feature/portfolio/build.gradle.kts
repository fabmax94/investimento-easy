plugins {
    alias(libs.plugins.investimento.android.feature)
}

android {
    namespace = "com.investimentoeasy.feature.portfolio"
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}
