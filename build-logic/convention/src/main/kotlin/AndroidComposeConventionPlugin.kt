import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Compose + testes de UI na JVM (Robolectric) + screenshots (Roborazzi). */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("io.github.takahirom.roborazzi")
        val android = extensions.getByName("android") as CommonExtension<*, *, *, *, *, *>
        android.buildFeatures.compose = true

        dependencies {
            val bom = platform(libs.lib("compose-bom"))
            add("implementation", bom)
            add("testImplementation", bom)
            add("implementation", libs.lib("compose-ui"))
            add("implementation", libs.lib("compose-material3"))
            add("implementation", libs.lib("compose-ui-tooling-preview"))
            add("debugImplementation", libs.lib("compose-ui-tooling"))
            add("debugImplementation", libs.lib("compose-ui-test-manifest"))
            add("testImplementation", libs.lib("compose-ui-test-junit4"))
            add("testImplementation", libs.lib("roborazzi"))
            add("testImplementation", libs.lib("roborazzi-compose"))
            add("testImplementation", libs.lib("roborazzi-junit-rule"))
        }
    }
}
