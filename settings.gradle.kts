pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "investimento-easy"

// Módulos Kotlin/JVM puros: regras de negócio, sem dependência do Android.
include(":core:model")
include(":core:domain")
include(":core:testing")
include(":parser:xperformance")

// Módulos Android (:app, :core:designsystem, :feature:*) entram quando o
// repositório Google Maven estiver acessível no ambiente de build (ver T0.4).
