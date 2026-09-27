plugins {
    alias(libs.plugins.investimento.coverage.aggregation)
}

// Cobertura agregada (mínimo verificado em `./gradlew check`).
// :core:testing fica de fora: são fakes e builders de teste.
dependencies {
    kover(projects.core.model)
    kover(projects.core.domain)
    kover(projects.core.importacao)
    kover(projects.core.ai)
    kover(projects.parser.xperformance)
    kover(projects.core.database)
    kover(projects.core.documentos)
    kover(projects.core.designsystem)
    kover(projects.core.ui)
    kover(projects.feature.upload)
    kover(projects.feature.portfolio)
    kover(projects.app)
}
