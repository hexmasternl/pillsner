package nl.hexmaster.pillsner.ui.medicines.history

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The usage history screen holds a whole medicine's record, so the same rule as the reminders and
 * the watch-sync packages applies to it: debug level only, and a log line may name the identifier
 * that failed to open, never the medicine or an amount (spec: medicine-usage-history, "Nothing
 * sensitive is logged").
 *
 * Reading the source catches the mistake at review time rather than on a user's device.
 */
class MedicineHistoryLoggingTest {

    @Test
    fun theHistoryPackage_neverLogsAboveDebugLevel() {
        val offenders = sources().flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> ABOVE_DEBUG.containsMatchIn(line) }
                .map { (index, line) -> "${file.name}:${index + 1}: ${line.trim()}" }
        }

        assertEquals("The usage history must not log above debug level", emptyList<String>(), offenders)
    }

    @Test
    fun theHistoryPackage_neverInterpolatesAMedicineIntoALogMessage() {
        val offenders = sources().flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> ANY_LOG.containsMatchIn(line) && INTERPOLATION.containsMatchIn(line) }
                .filterNot { (_, line) -> SAFE_INTERPOLATIONS.any { it in line } }
                .map { (index, line) -> "${file.name}:${index + 1}: ${line.trim()}" }
        }

        assertEquals(
            "A usage history log line may carry the medicine's identifier, never its name or amount",
            emptyList<String>(),
            offenders,
        )
    }

    private fun sources(): List<File> {
        val packageDir = File("src/main/java/nl/hexmaster/pillsner/ui/medicines/history")
        assertTrue("Expected sources at ${packageDir.absolutePath}", packageDir.isDirectory)
        return packageDir.listFiles { file -> file.extension == "kt" }?.toList().orEmpty()
    }

    private companion object {
        val ABOVE_DEBUG = Regex("""\bLog\.(i|w|e|wtf)\s*\(""")
        val ANY_LOG = Regex("""\bLog\.[a-z]+\s*\(""")
        val INTERPOLATION = Regex("""\$""")

        /** Which medicine failed to open, by number. Nothing that says what it is. */
        val SAFE_INTERPOLATIONS = listOf("\${medicationId.value}")
    }
}
