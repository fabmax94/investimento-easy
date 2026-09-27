plugins {
    alias(libs.plugins.investimento.android.feature)
}

android {
    namespace = "com.investimentoeasy.feature.upload"
}

dependencies {
    implementation(projects.core.documentos)
    testImplementation(testFixtures(projects.parser.xperformance))
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}
