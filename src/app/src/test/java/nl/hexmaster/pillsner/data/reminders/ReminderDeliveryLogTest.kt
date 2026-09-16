package nl.hexmaster.pillsner.data.reminders

import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.hexmaster.pillsner.domain.MutableTestClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The delivery log holds the order things happened, survives being read back, and never grows
 * without bound. It is shown on a Settings screen and copied out of the app, so what it holds is
 * also what a person will read.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderDeliveryLogTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val clock = MutableTestClock(Instant.parse("2026-09-15T06:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `entries come back in the order they were recorded, with what was recorded`() = runTest {
        val log = logIn(folder.newFile())

        log.record(DeliveryEvent.WAKE, "ALARM")
        clock.setTo(Instant.parse("2026-09-15T06:00:02Z"))
        log.record(DeliveryEvent.POSTED, "41")
        log.record(DeliveryEvent.ALARMS_ARMED)
        testScheduler.advanceUntilIdle()

        assertEquals(
            listOf(
                ReminderDeliveryLog.Entry(Instant.parse("2026-09-15T06:00:00Z"), DeliveryEvent.WAKE, "ALARM"),
                ReminderDeliveryLog.Entry(Instant.parse("2026-09-15T06:00:02Z"), DeliveryEvent.POSTED, "41"),
                ReminderDeliveryLog.Entry(Instant.parse("2026-09-15T06:00:02Z"), DeliveryEvent.ALARMS_ARMED, null),
            ),
            log.observe().first(),
        )
    }

    @Test
    fun `the log survives a fresh process reading the same file`() = runTest {
        val file = folder.newFile()
        logIn(file).record(DeliveryEvent.LAPSED_UNANNOUNCED, "40")
        testScheduler.advanceUntilIdle()

        val reopened = logIn(file)

        assertEquals(listOf(DeliveryEvent.LAPSED_UNANNOUNCED), reopened.observe().first().map { it.event })
        assertEquals("2026-09-15T06:00:00Z\tLAPSED_UNANNOUNCED\t40", reopened.asText())
    }

    @Test
    fun `the log is bounded and keeps the newest entries`() = runTest {
        val log = logIn(folder.newFile())

        repeat(ReminderDeliveryLog.MAX_ENTRIES + 200) { log.record(DeliveryEvent.WAKE, it.toString()) }
        testScheduler.advanceUntilIdle()

        val entries = log.observe().first()
        assertTrue(entries.size <= ReminderDeliveryLog.MAX_ENTRIES + 50)
        assertEquals((ReminderDeliveryLog.MAX_ENTRIES + 199).toString(), entries.last().detail)
    }

    @Test
    fun `the log does not re-read the file's size on every record, only tracks it in memory`() = runTest {
        val file = folder.newFile()
        val log = logIn(file)
        log.record(DeliveryEvent.WAKE, "ALARM")
        testScheduler.advanceUntilIdle()

        // Grown far past the trim threshold, but not through the log: if append() re-read the
        // file's actual size before deciding whether to trim, it would notice this and trim.
        // Growing it behind the log's back and then seeing it *not* trim is exactly the evidence
        // that the log tracks its own count in memory rather than asking the file every time.
        file.writeText((1..ReminderDeliveryLog.MAX_ENTRIES + 100).joinToString("\n") { "not a log line $it" } + "\n")

        repeat(10) { log.record(DeliveryEvent.WAKE, it.toString()) }
        testScheduler.advanceUntilIdle()

        assertTrue(
            "The log trusted its in-memory count instead of the file's real size and did not trim",
            file.readLines().size > ReminderDeliveryLog.MAX_ENTRIES + 100,
        )
    }

    @Test
    fun `trimming still happens once the log's own count crosses the threshold`() = runTest {
        val log = logIn(folder.newFile())

        repeat(ReminderDeliveryLog.MAX_ENTRIES + 60) { log.record(DeliveryEvent.WAKE, it.toString()) }
        testScheduler.advanceUntilIdle()

        val entries = log.observe().first()
        assertTrue(
            "Trimming should have brought the log back down near MAX_ENTRIES",
            entries.size <= ReminderDeliveryLog.MAX_ENTRIES + 50,
        )
        assertEquals((ReminderDeliveryLog.MAX_ENTRIES + 59).toString(), entries.last().detail)
    }

    @Test
    fun `a line the app cannot read is skipped rather than failing the whole log`() = runTest {
        val file = folder.newFile()
        file.writeText("not a log line\n2026-09-15T06:00:00Z\tPOSTED\t7\n")

        assertEquals(listOf("7"), logIn(file).observe().first().map { it.detail })
    }

    private fun kotlinx.coroutines.test.TestScope.logIn(file: File) =
        ReminderDeliveryLog(file, clock, StandardTestDispatcher(testScheduler))
}
