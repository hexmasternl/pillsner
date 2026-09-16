import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Signing material comes from src/keystore.properties on a developer machine and from environment
// variables in CI. Neither is committed; see docs/publishing-to-google-play.md.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(property: String, environmentVariable: String): String? =
    keystoreProperties.getProperty(property)
        ?: providers.environmentVariable(environmentVariable).orNull

val keystorePath: String? = signingValue("storeFile", "PILLSNER_KEYSTORE_PATH")

// One number decides the version code of both applications; see Part 5 of the publishing guide.
val versionCodeBase: Int =
    providers.environmentVariable("PILLSNER_VERSION_CODE").orNull?.toInt() ?: 1
val releaseVersionName: String =
    providers.environmentVariable("PILLSNER_VERSION_NAME").orNull ?: "0.1.0"

android {
    namespace = "nl.hexmaster.pillsner"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        applicationId = "nl.hexmaster.pillsner"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        // The phone application takes the even slot; the watch takes the one above it.
        versionCode = versionCodeBase * 10
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // The two languages the app ships. Strips every other locale from library resources, and
        // makes the fallback chain exactly values-nl to values (English).
        resourceConfigurations += listOf("en", "nl")
    }

    signingConfigs {
        // Only created when signing material is present, so a plain local `bundleRelease` still
        // produces an (unsigned) artifact instead of failing the configuration phase.
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = signingValue("storePassword", "PILLSNER_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "PILLSNER_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "PILLSNER_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    // Room exports every schema version here so migrations can be tested against them
    // (app-medicine-add design D3).
    sourceSets.getByName("androidTest") {
        assets.srcDirs("$projectDir/schemas")
    }

    buildFeatures {
        compose = true
        // Generates BuildConfig, so the version and the application id the About screen reports
        // come from the build rather than from text kept in step by hand (app-about-screen D2).
        buildConfig = true
    }

    lint {
        lintConfig = file("lint.xml")
    }

    bundle {
        language {
            // Every supported language ships in the base install. Play would otherwise deliver
            // only the one the phone is set to, and the in-app language picker offers the rest.
            enableSplit = false
        }
    }

    testOptions {
        unitTests {
            // The view models log at debug level. Without this every android.util.Log call throws
            // "not mocked" and a unit test would be asserting on the stub rather than the code.
            isReturnDefaultValues = true
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
    }
}

dependencies {
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // AndroidX
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.process)
    // The periodic watchdog that repairs a broken alarm chain (reminder-delivery-reliability D3).
    implementation(libs.androidx.work.runtime.ktx)
    // Required by androidx.biometric 1.1.0: BiometricPrompt needs a FragmentActivity host.
    implementation(libs.androidx.fragment.ktx)

    // Room: on-device storage. KSP runs its compiler; see design D9.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Kotlin
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Wear OS: the phone half of the phone-to-watch sync (app-wearable-support design D9).
    implementation(project(":shared"))
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
