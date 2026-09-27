plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.investimento.coverage.aggregation)
}

// Cobertura agregada (mínimo verificado em `./gradlew check`).
// :core:testing fica de fora: são fakes e builders de teste.
dependencies {
    kover(projects.core.model)
    kover(projects.core.domain)
    kover(projects.parser.xperformance)
}
