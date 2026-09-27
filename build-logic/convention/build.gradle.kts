plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.kover.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("jvmLibrary") {
            id = "investimento.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("coverageAggregation") {
            id = "investimento.coverage.aggregation"
            implementationClass = "CoverageAggregationConventionPlugin"
        }
    }
}
