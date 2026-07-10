plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace  = "io.github.jmsdnns.androidmtrls"
    compileSdk = 36

    defaultConfig {
        minSdk = 31
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
    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)

    // Internal glyphs only — no Material3
    implementation(libs.material.icons.core)

    // Low-level ripple node backing amRipple() — no Material3
    implementation(libs.material.ripple)

    // BackHandler / PredictiveBackHandler for overlay + panel dismissal
    implementation(libs.activity.compose)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Graphics shapes (corner smoothing, Phase 12)
    implementation(libs.graphics.shapes)

    // Reorder engine for AMReorderableRowList / AMReorderableCardList (Phase 8)
    implementation(libs.reorderable)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit4)
}
