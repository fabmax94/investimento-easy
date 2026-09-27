plugins {
    alias(libs.plugins.investimento.jvm.library)
    `java-test-fixtures`
}

dependencies {
    api(projects.core.model)
}

// Planilhas reais opcionais (fora do git): o diretório pode não existir, como no CI.
val localFixtures = rootProject.layout.projectDirectory.dir("local-fixtures")
tasks.withType<Test>().configureEach {
    systemProperty("localFixturesDir", localFixtures.asFile.absolutePath)
    inputs
        .files(fileTree(localFixtures) { include("*.xlsx") })
        .withPropertyName("localFixtures")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
