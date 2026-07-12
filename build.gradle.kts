plugins {
    // AGP 9 provides built-in Kotlin support, so the standalone
    // org.jetbrains.kotlin.android plugin is intentionally not applied.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
