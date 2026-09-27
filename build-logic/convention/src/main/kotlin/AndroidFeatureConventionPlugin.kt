import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Módulo de feature: biblioteca Android + Compose + Hilt + design system + domínio. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("investimento.android.library")
        pluginManager.apply("investimento.android.compose")
        pluginManager.apply("investimento.hilt")
        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:domain"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("androidx-hilt-navigation-compose"))
            add("implementation", libs.lib("androidx-navigation-compose"))
            add("testImplementation", project(":core:testing"))
        }
    }
}
