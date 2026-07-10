// Top-level build file. Configuration for sub-projects/modules lives in their
// own build.gradle.kts files. Version catalog is in gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library)     apply false
    alias(libs.plugins.kotlin.compose)      apply false
}
