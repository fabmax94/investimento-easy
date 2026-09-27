plugins {
    alias(libs.plugins.investimento.jvm.library)
}

dependencies {
    api(projects.core.model)
    testImplementation(projects.core.testing)
}

dependencies {
    testImplementation(libs.kotlinx.coroutines.test)
}
