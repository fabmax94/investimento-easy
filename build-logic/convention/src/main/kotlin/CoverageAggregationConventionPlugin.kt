import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Aplicado na raiz: agrega a cobertura dos módulos (declarados com `kover(...)`) e
 * verifica o mínimo em `check`. Agregar evita punir um módulo de modelo cujas classes são
 * exercitadas pelos testes de outros módulos (ex.: o parser).
 */
class CoverageAggregationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("base")
        pluginManager.apply("org.jetbrains.kotlinx.kover")

        extensions.configure<KoverProjectExtension> {
            reports {
                filters {
                    excludes {
                        // Código gerado por Hilt, Room e Compose: não é nosso para testar.
                        packages("hilt_aggregated_deps", "dagger.hilt.internal.aggregatedroot.codegen")
                        classes("*_Factory", "*_Factory\$*", "*_HiltModules*", "Hilt_*", "*_MembersInjector", "*_Impl", "*_Impl\$*")
                        classes("*ComposableSingletons*", "*.BuildConfig")
                        annotatedBy("dagger.Module")
                    }
                }
                verify {
                    rule {
                        minBound(MIN_LINE_COVERAGE)
                    }
                }
            }
        }
        tasks.named("check").configure { dependsOn("koverVerify") }
    }

    private companion object {
        const val MIN_LINE_COVERAGE = 85
    }
}
