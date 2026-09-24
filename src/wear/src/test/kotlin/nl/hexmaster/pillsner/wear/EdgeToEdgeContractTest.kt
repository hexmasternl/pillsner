package nl.hexmaster.pillsner.wear

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every activity draws edge-to-edge, on every Android version Pillsner supports.
 *
 * Android 15 forces it on apps that target API 35 and later, and Android 16 removes the opt-out,
 * so an activity that does not ask for it explicitly behaves differently below API 35 and gets
 * flagged by Google Play ("Edge-to-edge may not display for all users", issue #72). The
 * `app-theme` spec therefore requires every activity to call `enableEdgeToEdge()` before
 * `super.onCreate()`. This test scans the source of every non-test source set for activity classes
 * and holds them to that, so a future activity fails the build rather than the next Play review.
 *
 * It looks for one direct call, deliberately: a single obvious line at the top of `onCreate` is the
 * convention, and a helper that hides it would defeat the point of the guard.
 */
class EdgeToEdgeContractTest {

    @Test
    fun everyActivity_enablesEdgeToEdgeBeforeSuperOnCreate() {
        val activities = productionSources().filter { ACTIVITY_CLASS.containsMatchIn(it.readText()) }
        assertTrue("Expected at least one activity under ${sourceRoot().absolutePath}", activities.isNotEmpty())

        val offenders = activities.mapNotNull { file ->
            // Comments are dropped first: MainActivity's KDoc talks about the call, which is not the same as making it.
            val source = file.readText().replace(COMMENT, "")
            val enable = source.indexOf("enableEdgeToEdge(")
            val superCreate = source.indexOf("super.onCreate(")
            when {
                enable < 0 -> "${file.relativePath()}: never calls enableEdgeToEdge()"
                superCreate < 0 -> "${file.relativePath()}: has no super.onCreate() to order it against"
                enable > superCreate -> "${file.relativePath()}: calls enableEdgeToEdge() after super.onCreate()"
                else -> null
            }
        }

        assertEquals(RULE, emptyList<String>(), offenders)
    }

    @Test
    fun nothingOptsOutOfEdgeToEdgeEnforcement() {
        val offenders = sourceRoot().walkTopDown()
            .filter { it.isFile && it.extension == "xml" && !it.path.contains("${File.separator}test") }
            .filter { it.readText().contains(OPT_OUT_ATTRIBUTE) }
            .map { it.relativePath() }
            .toList()

        assertEquals(RULE, emptyList<String>(), offenders)
    }

    /** Every `.kt` file under `src/<sourceSet>/` except the test source sets. */
    private fun productionSources(): List<File> = sourceRoot().listFiles()
        .orEmpty()
        .filter { it.isDirectory && !it.name.startsWith("test") && !it.name.startsWith("androidTest") }
        .flatMap { sourceSet -> sourceSet.walkTopDown().filter { it.isFile && it.extension == "kt" } }

    /** Unit tests run with the module directory as the working directory, like the build script guard does. */
    private fun sourceRoot(): File {
        val root = File("src")
        assertTrue("Expected the module's source root at ${root.absolutePath}", root.isDirectory)
        return root
    }

    private fun File.relativePath() = relativeTo(File(".")).path

    private companion object {
        const val RULE = "Every activity calls enableEdgeToEdge() before super.onCreate() and nothing opts out " +
            "(app-theme spec, \"Every shipped activity enables edge-to-edge\")."
        const val OPT_OUT_ATTRIBUTE = "windowOptOutEdgeToEdgeEnforcement"

        /** A class whose supertype list names some `...Activity(` constructor. */
        val ACTIVITY_CLASS = Regex("""\bclass\s+\w+\s*(?:\([^)]*\))?\s*:\s*[^{]*?\w*Activity\s*\(""")

        /** Block comments (KDoc included) and line comments. */
        val COMMENT = Regex("""/\*[\s\S]*?\*/|//[^\n]*""")
    }
}
