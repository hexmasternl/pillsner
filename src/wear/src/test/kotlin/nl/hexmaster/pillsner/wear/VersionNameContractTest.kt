package nl.hexmaster.pillsner.wear

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The version the agenda ends with comes from the build, not from text written in code.
 *
 * Spec: wearable-app, "App mark and version on the agenda" — the version name MUST come from the
 * build configuration, so that raising it there is all it takes ("Version bump is reflected
 * without code changes"). A literal in its place would look right until the next release and then
 * tell users, and bug reports, the wrong build.
 *
 * Like `EdgeToEdgeContractTest`, this reads the source: what the activity hands to the UI is one
 * line in `MainActivity.kt`, and no Compose test reaches it, because those render the screen with a
 * version of their own.
 */
class VersionNameContractTest {

    private val mainActivity: String by lazy {
        val file = File("src/main/kotlin/nl/hexmaster/pillsner/wear/MainActivity.kt")
        assertTrue("Expected the watch activity at ${file.absolutePath}", file.isFile)
        // Comments are dropped first: describing the rule is not the same as following it.
        file.readText().replace(COMMENT, "")
    }

    @Test
    fun theActivity_handsTheBuildsVersionNameToTheApp() {
        assertTrue(
            "$RULE MainActivity must call WearApp(..., BuildConfig.VERSION_NAME).",
            WEAR_APP_WITH_BUILD_VERSION.containsMatchIn(mainActivity),
        )
    }

    @Test
    fun theApp_passesThatVersionOnToTheAgenda() {
        assertTrue(
            "$RULE WearApp must take appVersion as a parameter.",
            WEAR_APP_TAKES_VERSION.containsMatchIn(mainActivity),
        )
        assertTrue(
            "$RULE WearApp must pass its appVersion parameter to UpcomingDosesScreen.",
            AGENDA_WITH_PASSED_VERSION.containsMatchIn(mainActivity),
        )
    }

    @Test
    fun theActivity_writesNoVersionNameOfItsOwn() {
        val literals = VERSION_LITERAL.findAll(mainActivity).map { it.value }.toList()
        assertTrue("$RULE MainActivity contains version-like text: $literals", literals.isEmpty())
    }

    private companion object {
        const val RULE = "The watch version comes from the build configuration " +
            "(wearable-app spec, \"App mark and version on the agenda\")."

        /** The call in `setContent`, with the build's version name as its last argument. */
        val WEAR_APP_WITH_BUILD_VERSION = Regex("""WearApp\(\s*[^()]*,\s*BuildConfig\.VERSION_NAME\s*,?\s*\)""")

        /** The declaration of `WearApp`, with a plain `appVersion: String` parameter and no default. */
        val WEAR_APP_TAKES_VERSION = Regex("""fun\s+WearApp\([^)]*\bappVersion:\s*String\s*,?\s*\)""")

        /** The agenda gets `appVersion` as its second argument, positionally or by name. */
        val AGENDA_WITH_PASSED_VERSION =
            Regex("""UpcomingDosesScreen\(\s*\w+\s*,\s*(?:appVersion\s*=\s*)?appVersion\s*[,)]""")

        /** A quoted string that reads like a version name: "0.1.0", "1.2". */
        val VERSION_LITERAL = Regex(""""\d+(?:\.\d+)+"""")

        /** Block comments (KDoc included) and line comments. */
        val COMMENT = Regex("""/\*[\s\S]*?\*/|//[^\n]*""")
    }
}
