import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Módulo de feature: biblioteca Android + Compose + Hilt + design system + domínio. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("investimento.android.library")
        pluginManager.apply("investimento.android.compose")
        pluginManager.apply("investimento.hilt")
        // Rotas de navegação tipadas (@Serializable).
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        dependencies {
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:common"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("androidx-hilt-navigation-compose"))
            add("implementation", libs.lib("androidx-navigation-compose"))
            add("testImplementation", project(":core:testing"))
        }
    }
}
