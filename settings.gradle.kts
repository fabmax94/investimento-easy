pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
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
include(":core:importacao")
include(":core:ai")

// Módulos Android.
include(":app")
include(":core:designsystem")
include(":core:database")
include(":core:documentos")
include(":core:common")
include(":core:ui")
include(":core:seguranca")
include(":feature:upload")
include(":feature:portfolio")
include(":feature:analysis")
