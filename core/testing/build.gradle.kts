plugins {
    alias(libs.plugins.investimento.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(projects.core.domain)
}
