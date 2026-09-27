import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal const val COMPILE_SDK = 36
internal const val MIN_SDK = 26

/** Configuração comum a app e bibliotecas Android. Testes locais usam JUnit 4 (Robolectric e Compose). */
internal fun Project.configurarAndroid(android: CommonExtension<*, *, *, *, *, *>) {
    pluginManager.apply("org.jetbrains.kotlin.android")
    configurarQualidade()

    android.apply {
        compileSdk = COMPILE_SDK
        defaultConfig.minSdk = MIN_SDK
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        testOptions.unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = false
        }
        lint {
            warningsAsErrors = true
            abortOnError = true
            checkDependencies = true
            lintConfig = rootProject.file("config/lint/lint.xml")
        }
    }
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(true)
        }
    }
    dependencies {
        add("testImplementation", libs.lib("junit4"))
        add("testImplementation", libs.lib("kotest-assertions"))
        add("testImplementation", libs.lib("mockk"))
        add("testImplementation", libs.lib("kotlinx-coroutines-test"))
        add("testImplementation", libs.lib("robolectric"))
        add("testImplementation", libs.lib("androidx-test-core"))
        add("testImplementation", libs.lib("androidx-test-ext-junit"))
    }
    tasks.withType<Test>().configureEach {
        testLogging {
            events("failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        // Robolectric: evita baixar o android-all em tempo de teste por um mirror diferente.
        systemProperty("robolectric.logging", "stdout")
    }
}
