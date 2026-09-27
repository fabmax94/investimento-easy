plugins {
    alias(libs.plugins.investimento.jvm.library)
}

dependencies {
    api(projects.core.domain)
    implementation(projects.parser.xperformance)
    implementation(projects.parser.xlsx)
    testImplementation(projects.core.testing)
    testImplementation(testFixtures(projects.parser.xperformance))
    testImplementation(testFixtures(projects.parser.xlsx))
}
