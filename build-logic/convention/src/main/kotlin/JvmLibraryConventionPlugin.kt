import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Convenção para módulos Kotlin/JVM puros (regras de negócio, parsers).
 *
 * Bytecode alvo 17 para ser consumível pelos módulos Android. Aplica JUnit 5,
 * Kotest, MockK, detekt, ktlint e Kover (a cobertura mínima é verificada de forma agregada na raiz).
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        pluginManager.apply("org.jetbrains.kotlinx.kover")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        extensions.configure<KotlinJvmProjectExtension> {
            explicitApi()
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                allWarningsAsErrors.set(true)
            }
        }

        dependencies {
            add("testImplementation", platform(libs.findLibrary("junit-bom").get()))
            add("testImplementation", libs.findLibrary("junit-jupiter").get())
            add("testImplementation", libs.findLibrary("kotest-assertions").get())
            add("testImplementation", libs.findLibrary("kotest-property").get())
            add("testImplementation", libs.findLibrary("mockk").get())
            add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging {
                events("failed")
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        }

    }
}
