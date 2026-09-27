plugins {
    alias(libs.plugins.investimento.jvm.library)
}

dependencies {
    api(projects.core.domain)
    implementation(projects.parser.xperformance)
    testImplementation(projects.core.testing)
    testImplementation(testFixtures(projects.parser.xperformance))
}
