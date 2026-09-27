plugins {
    alias(libs.plugins.investimento.android.library)
    alias(libs.plugins.investimento.android.compose)
}

android {
    namespace = "com.investimentoeasy.core.designsystem"
}

dependencies {
    api(projects.core.model)
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}
