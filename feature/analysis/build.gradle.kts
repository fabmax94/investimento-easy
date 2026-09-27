plugins {
    alias(libs.plugins.investimento.android.feature)
}

android {
    namespace = "com.investimentoeasy.feature.analysis"
}

dependencies {
    implementation(projects.core.ai)
    implementation(projects.core.seguranca)
    testImplementation(testFixtures(projects.parser.xperformance))
    testImplementation(projects.core.importacao)
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}
