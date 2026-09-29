pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com[.]android.*")
                includeGroupByRegex("com[.]google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Tesseract4Android (medicine-label-photo-prefill design D1) is published through JitPack, not
        // Maven Central. The content filter admits only that one group, so nothing else can ever
        // resolve from here, and the manifest guard in app/build.gradle.kts pins the AAR's checksum.
        maven("https://jitpack.io") {
            content { includeGroup("cz.adaptech.tesseract4android") }
        }
    }
}

rootProject.name = "Pillsner"
include(":app")
include(":shared")
include(":wear")
