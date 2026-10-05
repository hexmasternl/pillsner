package nl.hexmaster.pillsner.data.reminders

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A refused foreground promotion is survived and recorded, so the service goes on to run the work
 * it was started for instead of crashing the process (fix-boot-wake-service-crash design D2).
 */
class ForegroundPromotionTest {

    private val log = RecordingLog()

    @Test
    fun `an accepted promotion records nothing`() {
        val promoted = promoteToForeground(promote = {}, log = log, detail = "ALARM")

        assertTrue(promoted)
        assertTrue(log.records.isEmpty())
    }

    @Test
    fun `a promotion refused from a boot broadcast is survived and logged with the reason`() {
        // ForegroundServiceStartNotAllowedException is an IllegalStateException; this is its message.
        val promoted = promoteToForeground(
            promote = { throw IllegalStateException("FGS type shortService not allowed to start from BOOT_COMPLETED!") },
            log = log,
            detail = "BOOT",
        )

        assertFalse(promoted)
        assertEquals(listOf(DeliveryEvent.SERVICE_REFUSED to "BOOT"), log.records)
    }

    @Test
    fun `a promotion refused for a missing permission is survived and logged`() {
        val promoted = promoteToForeground(
            promote = { throw SecurityException("missing FOREGROUND_SERVICE") },
            log = log,
            detail = "ANSWER",
        )

        assertFalse(promoted)
        assertEquals(listOf(DeliveryEvent.SERVICE_REFUSED to "ANSWER"), log.records)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `anything that is not a refusal is a bug and is thrown on`() {
        promoteToForeground(promote = { throw IllegalArgumentException() }, log = log, detail = "ALARM")
    }

    /** A log that keeps what it was told in memory instead of writing a file. */
    private class RecordingLog : ReminderDeliveryLog(File("unused")) {
        val records = mutableListOf<Pair<DeliveryEvent, String?>>()

        override fun record(event: DeliveryEvent, detail: String?) {
            records += event to detail
        }
    }
}
