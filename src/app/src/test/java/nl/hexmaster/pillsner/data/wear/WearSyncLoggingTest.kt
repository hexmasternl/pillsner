package nl.hexmaster.pillsner.data.wear

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The same rule as the reminders package, for the same reason: a medicine's name is the most
 * private thing this app holds, and the sync code handles the whole list of them. So it logs at
 * debug level only, and a log line may say how many doses went out, never which.
 *
 * The watch module is checked here too, from the one place both sources are reachable.
 */
class WearSyncLoggingTest {

    @Test
    fun theSyncCode_neverLogsAboveDebugLevel() {
        val offenders = sources().flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> ABOVE_DEBUG.containsMatchIn(line) }
                .map { (index, line) -> "${file.name}:${index + 1}: ${line.trim()}" }
        }

        assertEquals("Watch sync must not log above debug level", emptyList<String>(), offenders)
    }

    @Test
    fun theSyncCode_neverInterpolatesAMedicineIntoALogMessage() {
        val offenders = sources().flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> ANY_LOG.containsMatchIn(line) && INTERPOLATION.containsMatchIn(line) }
                .filterNot { (_, line) -> SAFE_INTERPOLATIONS.any { it in line } }
                .map { (index, line) -> "${file.name}:${index + 1}: ${line.trim()}" }
        }

        assertEquals(
            "A sync log line may carry a count, a status or an error type, never a medicine",
            emptyList<String>(),
            offenders,
        )
    }

    private fun sources(): List<File> {
        val directories = listOf(
            File("src/main/java/nl/hexmaster/pillsner/data/wear"),
            File("../wear/src/main/kotlin/nl/hexmaster/pillsner/wear"),
        )
        directories.forEach { assertTrue("Expected sources at ${it.absolutePath}", it.isDirectory) }
        return directories.flatMap { it.walkTopDown().filter { file -> file.extension == "kt" } }
    }

    private companion object {
        val ABOVE_DEBUG = Regex("""\bLog\.(i|w|e|wtf)\s*\(""")
        val ANY_LOG = Regex("""\bLog\.[a-z]+\s*\(""")
        val INTERPOLATION = Regex("""\$""")

        /** How many, how it went, and what kind of error. Nothing that names a medicine. */
        val SAFE_INTERPOLATIONS = listOf(
            "\${payload.doses.size}",
            "\$status",
            "\${error::class.simpleName}",
        )
    }
}
