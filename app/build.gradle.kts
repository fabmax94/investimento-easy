plugins {
    alias(libs.plugins.investimento.android.application)
    alias(libs.plugins.investimento.android.compose)
    alias(libs.plugins.investimento.hilt)
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.investimentoeasy.app"

    defaultConfig {
        applicationId = "com.investimentoeasy.app"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            // Metadados duplicados das dependências do SDK do Claude (httpclient5, jackson).
            excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/INDEX.LIST")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.documentos)
    implementation(projects.feature.upload)
    implementation(projects.feature.portfolio)
    implementation(projects.feature.analysis)
    implementation(projects.core.mercado)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}
