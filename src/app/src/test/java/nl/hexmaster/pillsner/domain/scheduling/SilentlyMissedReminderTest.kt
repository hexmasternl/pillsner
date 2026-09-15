package nl.hexmaster.pillsner.domain.scheduling

import java.time.Duration
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.amsterdam
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.at
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.dose
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.repositoryWith
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.today
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rule the Home banner is raised by: a dose that lapsed without the user ever having been told
 * about it, although the app had a window in which to tell them (spec:
 * reminder-delivery-resilience, "A silently missed reminder is recorded").
 *
 * The rule lives on [nl.hexmaster.pillsner.domain.model.Dose] so it can be read here without a
 * device. The one part that is not on the dose — whether notifications were allowed at all — is the
 * coordinator's guard, and is covered by the last test here against the same shape.
 */
class SilentlyMissedReminderTest {

    /** A dose stored yesterday for eight this morning: the app had all night to announce it. */
    private val plannedYesterday = at(today.minusDays(1), hour = 20)

    @Test
    fun `a dose that lapsed with no reminder ever posted is a silently missed reminder`() = runBlocking {
        val doses = repositoryWith(
            dose(1, at(hour = 8), plannedAt = plannedYesterday),
            dose(2, at(hour = 20), plannedAt = plannedYesterday),
        )
        val clock = MutableTestClock(at(hour = 20), amsterdam)

        val lapsed = MarkMissedDoses(doses, clock)()

        assertEquals(listOf(1L), lapsed.map { it.id.value })
        assertTrue(lapsed.single().wasMissedInSilence)
    }

    @Test
    fun `a dose that was reminded about and never answered is not`() = runBlocking {
        val doses = repositoryWith(
            dose(1, at(hour = 8), plannedAt = plannedYesterday, firstRemindedAt = at(hour = 8)),
            dose(2, at(hour = 20), plannedAt = plannedYesterday),
        )
        val clock = MutableTestClock(at(hour = 20), amsterdam)

        val lapsed = MarkMissedDoses(doses, clock)()

        assertEquals(listOf(1L), lapsed.map { it.id.value })
        assertFalse(lapsed.single().wasMissedInSilence)
    }

    @Test
    fun `a dose generated after its own moment is not`() = runBlocking {
        // The medicine was saved at eight in the evening; its eight o'clock dose this morning is
        // generated already lapsed, so there was never a reminder to lose.
        val savedThisEvening = at(hour = 20)
        val doses = repositoryWith(dose(1, at(hour = 8), plannedAt = savedThisEvening))
        val clock = MutableTestClock(at(today.plusDays(1), 9), amsterdam)

        val lapsed = MarkMissedDoses(doses, clock)()

        assertEquals(listOf(1L), lapsed.map { it.id.value })
        assertFalse(lapsed.single().wasMissedInSilence)
    }

    @Test
    fun `a dose planned in the same minute it was due is not`() = runBlocking {
        val eight = at(hour = 8)
        val doses = repositoryWith(dose(1, eight, plannedAt = eight))
        val clock = MutableTestClock(eight.plus(Duration.ofHours(25)), amsterdam)

        assertFalse(MarkMissedDoses(doses, clock)().single().wasMissedInSilence)
    }

    @Test
    fun `a dose still pending is not, however long ago it was planned`() = runBlocking {
        val pending = dose(1, at(hour = 8), plannedAt = plannedYesterday)

        assertFalse(pending.wasMissedInSilence)
    }

    @Test
    fun `the moment reported is the moment the dose lapsed`() = runBlocking {
        val doses = repositoryWith(
            dose(1, at(hour = 8), plannedAt = plannedYesterday),
            dose(2, at(hour = 20), plannedAt = plannedYesterday),
        )
        val clock = MutableTestClock(at(hour = 20), amsterdam)

        val lapsed = MarkMissedDoses(doses, clock)()

        // The eight o'clock dose lapses when the evening one falls due, not when the app noticed.
        assertEquals(at(hour = 20), silentlyMissedReminderAmong(lapsed, notificationsAllowed = true))
    }

    @Test
    fun `the most recent moment wins when several lapsed at once`() = runBlocking {
        val plannedTwoDaysAgo = at(today.minusDays(2), hour = 12)
        val doses = repositoryWith(
            dose(1, at(today.minusDays(1), hour = 8), medicationId = 1L, plannedAt = plannedTwoDaysAgo),
            dose(2, at(today.minusDays(1), hour = 20), medicationId = 2L, plannedAt = plannedTwoDaysAgo),
        )
        val clock = MutableTestClock(at(hour = 21), amsterdam)

        val lapsed = MarkMissedDoses(doses, clock)()

        // Neither medicine has a next dose, so each lapses a day after its own moment; the later
        // of the two is what the banner reports.
        assertEquals(2, lapsed.size)
        assertEquals(at(hour = 20), silentlyMissedReminderAmong(lapsed, notificationsAllowed = true))
    }

    @Test
    fun `nothing is reported while notifications are denied`() = runBlocking {
        val doses = repositoryWith(dose(1, at(hour = 8), plannedAt = plannedYesterday))
        val clock = MutableTestClock(at(today.plusDays(1), 9), amsterdam)

        val lapsed = MarkMissedDoses(doses, clock)()

        assertTrue(lapsed.single().wasMissedInSilence)
        assertNull(silentlyMissedReminderAmong(lapsed, notificationsAllowed = false))
    }

    @Test
    fun `nothing is reported when no dose lapsed in silence`() = runBlocking {
        val doses = repositoryWith(
            dose(1, at(hour = 8), plannedAt = plannedYesterday, firstRemindedAt = at(hour = 8)),
        )
        val clock = MutableTestClock(at(today.plusDays(1), 9), amsterdam)

        val lapsed = MarkMissedDoses(doses, clock)()

        assertNull(silentlyMissedReminderAmong(lapsed, notificationsAllowed = true))
    }
}
