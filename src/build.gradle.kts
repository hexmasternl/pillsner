// Root build script. Plugins are declared here (not applied) so every module resolves the same
// pinned versions from gradle/libs.versions.toml. Kotlin compilation itself is built into AGP 9;
// the org.jetbrains.kotlin.android plugin is therefore not applied anywhere.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
