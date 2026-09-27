plugins {
    alias(libs.plugins.investimento.jvm.library)
}

dependencies {
    api(projects.core.model)
    // Apenas para o teste local opcional com um PDF real (fora do git).
    // No app, o texto por página vem do PdfBox-Android.
    testImplementation(libs.pdfbox)
    // Teste de integração: extração -> classificação -> validação.
    testImplementation(projects.core.domain)
}

tasks.withType<Test>().configureEach {
    val dir = rootProject.layout.projectDirectory.dir("local-fixtures").asFile.absolutePath
    systemProperty("localFixturesDir", dir)
    inputs.dir(dir).optional().withPropertyName("localFixtures")
}
