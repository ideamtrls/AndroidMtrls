plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace   = "io.github.jmsdnns.androidmtrls.demo"
    compileSdk  = 36

    defaultConfig {
        applicationId = "io.github.jmsdnns.androidmtrls.demo"
        minSdk        = 31
        targetSdk     = 36
        versionCode   = 1
        versionName   = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":mtrls"))

    // Coil — demo-level ImageLoader config. The library depends on Coil with
    // `implementation` scope, so the demo needs its own compile-time dependency
    // to install a custom SingletonImageLoader that carries a descriptive
    // User-Agent (Wikimedia Commons 403s OkHttp's default UA). See DemoApplication.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.activity.compose)

    // Navigation Compose — demo-only (string-id registry navigation)
    implementation(libs.navigation.compose)

    // Material Icons Extended — demo-only icon substitutes for SF Symbols
    implementation(libs.material.icons.extended)

    debugImplementation(libs.compose.ui.tooling)
}
