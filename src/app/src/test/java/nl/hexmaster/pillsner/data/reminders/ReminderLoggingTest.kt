package nl.hexmaster.pillsner.data.reminders

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A medicine's name and dose are the most private things this app holds, and a log line is the
 * easiest place to leak them: logcat is readable by tooling, bug reports and anyone with the phone
 * plugged in.
 *
 * So the reminders package logs at debug level only, and never interpolates anything into a log
 * message. This test reads the source of the package and fails on the first line that does
 * otherwise, which catches the mistake at review time rather than on a user's device.
 */
class ReminderLoggingTest {

    @Test
    fun theRemindersPackage_neverLogsAboveDebugLevel() {
        val offenders = sources().flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> ABOVE_DEBUG.containsMatchIn(line) }
                .map { (index, line) -> "${file.name}:${index + 1}: ${line.trim()}" }
        }

        assertEquals("Reminders must not log above debug level", emptyList<String>(), offenders)
    }

    @Test
    fun theRemindersPackage_neverInterpolatesIntoALogMessage() {
        val offenders = sources().flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> ANY_LOG.containsMatchIn(line) && INTERPOLATION.containsMatchIn(line) }
                .filterNot { (_, line) -> SAFE_INTERPOLATIONS.any { it in line } }
                .map { (index, line) -> "${file.name}:${index + 1}: ${line.trim()}" }
        }

        assertEquals(
            "A log message may name a dose id or a wake reason, never a medicine or an amount",
            emptyList<String>(),
            offenders,
        )
    }

    private fun sources(): List<File> {
        val packageDir = File("src/main/java/nl/hexmaster/pillsner/data/reminders")
        assertTrue("The reminders package should be at ${packageDir.absolutePath}", packageDir.isDirectory)
        return packageDir.listFiles { file -> file.extension == "kt" }?.toList().orEmpty()
    }

    private companion object {
        val ABOVE_DEBUG = Regex("""\bLog\.(i|w|e|wtf)\s*\(""")
        val ANY_LOG = Regex("""\bLog\.[a-z]+\s*\(""")
        val INTERPOLATION = Regex("""\$""")

        /**
         * The only values a reminder log line may carry: which dose, what the user answered, why
         * the app woke, and the type of an error. None of them says what the medicine is.
         */
        val SAFE_INTERPOLATIONS = listOf(
            "\${doseId.value}",
            "\$action",
            "\$reason",
            "\${error::class.simpleName}",
            // How many dose history rows the retention purge removed: a count, never a medicine or
            // an amount (dose-history-retention design D1).
            "\$purged",
        )
    }
}
