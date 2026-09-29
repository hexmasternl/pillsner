import com.android.build.api.artifact.SingleArtifact
import java.security.MessageDigest
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
        // Every language the app ships (SupportedLanguages.all). Strips every other locale from
        // library resources; anything not listed here silently falls back to values (English).
        resourceConfigurations += listOf("en", "nl", "de", "fr", "es", "pt")
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

    // Label scanning (medicine-label-photo-prefill design D1, D2): the in-app viewfinder and the
    // on-device OCR engine. No ML Kit, no Firebase, no camera-mlkit-vision: the manifest guard at
    // the end of this file fails the build if any of them ever reaches the classpath.
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
    implementation(libs.tesseract4android.openmp)

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

// --- Manifest guard (medicine-label-photo-prefill design D8) -------------------------------------
//
// The first label-scan attempt shipped INTERNET and ACCESS_NETWORK_STATE without anyone editing
// Pillsner's manifest: a dependency's manifest merged them in. This task makes that impossible to
// miss. After every variant's manifest is processed it asserts that the merged permission set
// equals app/manifest-allowlist.txt, that no forbidden dependency group is on the runtime
// classpath, and that the OCR library's AAR has the checksum pinned in the same file. It is wired
// into check, assemble<Variant> and bundle<Variant>, so CI and the release workflow run it without
// a separate step.

/** The dependency groups that pulled Firebase and Google Play services into release 1.1.0. */
val forbiddenDependencyGroups = setOf(
    "com.google.firebase",
    "com.google.mlkit",
    "com.google.android.datatransport",
)

/** Individual artifacts that are forbidden by name whatever their group's status. */
val forbiddenDependencyArtifacts = setOf("androidx.camera:camera-mlkit-vision")

/**
 * The only Play services artifacts allowed: the wearable Data Layer the phone-to-watch sync uses
 * (app-wearable-support design D9) and its transitive base. Anything else from the group fails.
 */
val allowedPlayServicesArtifacts = setOf(
    "com.google.android.gms:play-services-wearable",
    "com.google.android.gms:play-services-base",
    "com.google.android.gms:play-services-basement",
    "com.google.android.gms:play-services-tasks",
)

/** The group the pinned OCR artifact resolves from. Its checksum line lives in the allow-list. */
val ocrArtifactGroup = "cz.adaptech.tesseract4android"

@DisableCachingByDefault(because = "A verification that takes well under a second and has no outputs")
abstract class VerifyManifestGuardTask : DefaultTask() {

    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:InputFile
    abstract val allowList: RegularFileProperty

    /** The variant's resolved runtime dependency graph, for the group and artifact checks. */
    @get:Input
    abstract val dependencyGraph: Property<ResolvedComponentResult>

    /** The resolved AAR files of the pinned OCR group, for the checksum check. */
    @get:InputFiles
    abstract val pinnedArtifactFiles: ConfigurableFileCollection

    @get:Input
    abstract val forbiddenGroups: SetProperty<String>

    @get:Input
    abstract val forbiddenArtifacts: SetProperty<String>

    @get:Input
    abstract val allowedPlayServices: SetProperty<String>

    @TaskAction
    fun verify() {
        val (allowedPermissions, pinnedChecksums) = readAllowList(allowList.get().asFile)
        val failures = mutableListOf<String>()
        failures += checkPermissions(allowedPermissions)
        failures += checkDependencies()
        failures += checkChecksums(pinnedChecksums)
        if (failures.isNotEmpty()) {
            throw GradleException(
                "Manifest guard failed for ${mergedManifest.get().asFile.parentFile.name}:\n" +
                    failures.joinToString("\n") { "  - $it" } +
                    "\nThe disclosed contract is app/manifest-allowlist.txt; see the README's permission table.",
            )
        }
        logger.lifecycle(
            "Manifest guard passed: ${allowedPermissions.size} permissions, " +
                "${pinnedChecksums.size} pinned artifact checksum(s), no forbidden dependency.",
        )
    }

    /** One permission per line, or an `artifact <group>:<name> <sha256>` line; `#` starts a comment. */
    private fun readAllowList(file: File): Pair<Set<String>, Map<String, String>> {
        val permissions = linkedSetOf<String>()
        val checksums = linkedMapOf<String, String>()
        file.readLines().forEach { raw ->
            val line = raw.substringBefore('#').trim()
            if (line.isEmpty()) return@forEach
            if (line.startsWith("artifact ")) {
                val parts = line.split(Regex("\\s+"))
                require(parts.size == 3) { "Malformed artifact line in ${file.name}: '$raw'" }
                checksums[parts[1]] = parts[2].lowercase()
            } else {
                permissions += line
            }
        }
        return permissions to checksums
    }

    private fun checkPermissions(allowed: Set<String>): List<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(mergedManifest.get().asFile)
        val declared = linkedSetOf<String>()
        // Both elements grant a permission; a library can merge in either.
        for (tag in listOf("uses-permission", "uses-permission-sdk-23")) {
            val elements = document.getElementsByTagName(tag)
            for (index in 0 until elements.length) {
                declared += elements.item(index).attributes.getNamedItem("android:name").nodeValue
            }
        }
        val undeclared = declared - allowed
        val missing = allowed - declared
        return undeclared.map { "Permission $it is in the merged manifest but not in the allow-list" } +
            missing.map { "Permission $it is in the allow-list but missing from the merged manifest" }
    }

    /** Walks the graph once, keeping the first path to every module so a failure can name it. */
    private fun checkDependencies(): List<String> {
        val forbiddenGroupSet = forbiddenGroups.get()
        val forbiddenArtifactSet = forbiddenArtifacts.get()
        val allowedGms = allowedPlayServices.get()
        val failures = mutableListOf<String>()
        val visited = mutableSetOf<ResolvedComponentResult>()

        fun visit(component: ResolvedComponentResult, path: List<String>) {
            if (!visited.add(component)) return
            component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { dependency ->
                val selected = dependency.selected
                val id = selected.id as? ModuleComponentIdentifier
                val label = id?.let { "${it.group}:${it.module}:${it.version}" } ?: selected.id.displayName
                val childPath = path + label
                if (id != null) {
                    val coordinates = "${id.group}:${id.module}"
                    val reason = when {
                        id.group in forbiddenGroupSet -> "its group is forbidden"
                        coordinates in forbiddenArtifactSet -> "it is forbidden by name"
                        id.group == "com.google.android.gms" && coordinates !in allowedGms ->
                            "only the wearable Play services artifacts are allowed"
                        else -> null
                    }
                    if (reason != null) {
                        failures += "Dependency $label is on the runtime classpath ($reason); path: " +
                            childPath.joinToString(" -> ")
                    }
                }
                visit(selected, childPath)
            }
        }
        visit(dependencyGraph.get(), listOf("app"))
        return failures.distinct()
    }

    private fun checkChecksums(pinned: Map<String, String>): List<String> {
        val files = pinnedArtifactFiles.files
        val resolvedAars = files.filter { it.extension == "aar" }
        // Every artifact the OCR group resolves must be pinned, not only the ones the allow-list
        // happens to name: an absent or partial pin set is not a passing one.
        val unpinned = resolvedAars.filter { file ->
            pinned.keys.none { coordinates -> file.name.startsWith(coordinates.substringAfter(':') + "-") }
        }
        val unpinnedFailures = unpinned.map { "Artifact ${it.name} is on the runtime classpath but app/manifest-allowlist.txt pins no checksum for it" }
        return unpinnedFailures + pinned.mapNotNull { (coordinates, expected) ->
            val artifactName = coordinates.substringAfter(':')
            val file = files.firstOrNull { it.name.startsWith("$artifactName-") && it.extension == "aar" }
                ?: return@mapNotNull "Artifact $coordinates has a pinned checksum but no AAR was resolved for it"
            val actual = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
                .joinToString("") { "%02x".format(it) }
            if (actual == expected) {
                null
            } else {
                "Artifact $coordinates (${file.name}) has SHA-256 $actual but the allow-list pins $expected"
            }
        }
    }
}

androidComponents {
    onVariants { variant ->
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val guard = tasks.register<VerifyManifestGuardTask>("verifyManifestGuard$variantName") {
            group = "verification"
            description = "Fails the $variantName build when its manifest, dependencies or OCR artifact " +
                "differ from app/manifest-allowlist.txt"
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            allowList.set(layout.projectDirectory.file("manifest-allowlist.txt"))
            dependencyGraph.set(variant.runtimeConfiguration.incoming.resolutionResult.rootComponent)
            pinnedArtifactFiles.from(
                variant.runtimeConfiguration.incoming.artifactView {
                    componentFilter { id -> id is ModuleComponentIdentifier && id.group == ocrArtifactGroup }
                    // The AAR as published, before AGP's transforms take it apart.
                    attributes { attribute(Attribute.of("artifactType", String::class.java), "aar") }
                }.files,
            )
            forbiddenGroups.set(forbiddenDependencyGroups)
            forbiddenArtifacts.set(forbiddenDependencyArtifacts)
            allowedPlayServices.set(allowedPlayServicesArtifacts)
        }
        // Registered lazily: the variant's assemble and bundle tasks do not exist yet at this point.
        tasks.matching { it.name == "assemble$variantName" || it.name == "bundle$variantName" }
            .configureEach { dependsOn(guard) }
        tasks.matching { it.name == "check" }.configureEach { dependsOn(guard) }
    }
}
