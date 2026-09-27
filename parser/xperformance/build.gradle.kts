plugins {
    alias(libs.plugins.investimento.jvm.library)
    `java-test-fixtures`
}

dependencies {
    api(projects.core.model)
    // Apenas para o teste local opcional com um PDF real (fora do git).
    // No app, o texto por página vem do PdfBox-Android.
    testImplementation(libs.pdfbox)
    // Teste de integração: extração -> classificação -> validação.
    testImplementation(projects.core.domain)
}

// Relatórios reais opcionais (fora do git): o diretório pode não existir, como no CI.
val localFixtures = rootProject.layout.projectDirectory.dir("local-fixtures")
tasks.withType<Test>().configureEach {
    systemProperty("localFixturesDir", localFixtures.asFile.absolutePath)
    inputs
        .files(fileTree(localFixtures) { include("*.pdf") })
        .withPropertyName("localFixtures")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
