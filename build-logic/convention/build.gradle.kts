plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.kover.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    // Aplicados só pelo id: ficam fora do classpath de compilação do build-logic
    // (o plugin do Hilt é compilado com Kotlin 2.2, mais novo que o do kotlin-dsl).
    runtimeOnly(libs.compose.gradle.plugin)
    runtimeOnly(libs.hilt.gradle.plugin)
    runtimeOnly(libs.roborazzi.gradle.plugin)
    runtimeOnly(libs.serialization.gradle.plugin)
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
        register("androidApplication") {
            id = "investimento.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "investimento.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "investimento.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "investimento.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("hilt") {
            id = "investimento.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("room") {
            id = "investimento.room"
            implementationClass = "RoomConventionPlugin"
        }
    }
}
