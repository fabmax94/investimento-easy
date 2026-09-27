plugins {
    alias(libs.plugins.investimento.android.library)
    alias(libs.plugins.investimento.hilt)
}

android {
    namespace = "com.investimentoeasy.core.documentos"
}

dependencies {
    api(projects.core.importacao)
    implementation(libs.pdfbox.android)
    testImplementation(projects.core.testing)
}

// Relatórios reais opcionais (fora do git), como no parser.
val localFixtures = rootProject.layout.projectDirectory.dir("local-fixtures")
tasks.withType<Test>().configureEach {
    systemProperty("localFixturesDir", localFixtures.asFile.absolutePath)
    inputs
        .files(fileTree(localFixtures) { include("*.pdf") })
        .withPropertyName("localFixtures")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
