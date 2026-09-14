import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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
    namespace = "nl.hexmaster.pillsner.wear"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        applicationId = "nl.hexmaster.pillsner"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        // The phone application takes the even slot; the watch takes the one above it.
        versionCode = versionCodeBase * 10 + 1
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // The two languages the app ships. Strips every other locale from library resources, and
        // makes the fallback chain exactly values-nl to values (English).
        resourceConfigurations += listOf("en", "nl")
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

    buildFeatures {
        compose = true
    }

    lint {
        lintConfig = file("lint.xml")
    }

    bundle {
        language {
            // Both languages ship in the base install, as on the phone: the watch renders in
            // whichever language the phone app is set to, which Play cannot know at install time.
            enableSplit = false
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
    }
}

dependencies {
    // The wire format, shared with the phone so the two cannot drift.
    implementation(project(":shared"))

    // Compose for Wear OS: its own Material 3, not the phone's.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.wear.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.wear.compose.ui.tooling)

    // The only supported phone-to-watch transport (design D9).
    implementation(libs.play.services.wearable)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
