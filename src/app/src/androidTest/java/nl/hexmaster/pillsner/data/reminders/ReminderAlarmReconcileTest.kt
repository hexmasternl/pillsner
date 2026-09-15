package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.domain.scheduling.WakeKind
import nl.hexmaster.pillsner.domain.scheduling.WakeMoment
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reconciling the armed alarms with the set the app now wants (spec: reminder-scheduling,
 * "Per-dose alarms and one housekeeping alarm").
 *
 * The property that matters is that reconciling is the whole repair: every wanted alarm is armed on
 * every call, so the watchdog has nothing to do but call this. `AlarmManager` will not say what it
 * currently holds, which is why the arm and cancel calls are watched here rather than the platform.
 */
@RunWith(AndroidJUnit4::class)
class ReminderAlarmReconcileTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = ArmedAlarmStore(context)

    private val morning = WakeMoment(Instant.parse("2026-09-14T06:00:00Z"), WakeKind.REMINDER)
    private val evening = WakeMoment(Instant.parse("2026-09-14T18:00:00Z"), WakeKind.REMINDER)
    private val housekeeping = WakeMoment(Instant.parse("2026-09-14T22:05:00Z"), WakeKind.HOUSEKEEPING)

    @Before
    fun clearTheRecord() = runBlocking { store.clear() }

    @After
    fun leaveNothingBehind() = runBlocking { store.clear() }

    @Test
    fun everyMomentGetsItsOwnAlarm() = runBlocking {
        val scheduler = WatchingScheduler()

        scheduler.reconcile(setOf(morning, evening, housekeeping))

        assertEquals(setOf(morning, evening, housekeeping), scheduler.armedMoments)
        assertEquals(setOf(morning, evening, housekeeping), store.armed())
    }

    @Test
    fun runningItTwiceOverTheSameScheduleCancelsNothing() = runBlocking {
        val scheduler = WatchingScheduler()
        scheduler.reconcile(setOf(morning, housekeeping))

        scheduler.reset()
        scheduler.reconcile(setOf(morning, housekeeping))

        // Every wanted alarm is armed again, because re-arming is the only thing that can repair
        // one the platform dropped — and it moves nothing when the alarm is already set.
        assertEquals(setOf(morning, housekeeping), scheduler.armedMoments)
        assertTrue("Nothing wanted may be cancelled", scheduler.cancelledMoments.isEmpty())
    }

    @Test
    fun anAnsweredDoseLosesItsAlarmAndTheOthersKeepTheirs() = runBlocking {
        val scheduler = WatchingScheduler()
        scheduler.reconcile(setOf(morning, evening, housekeeping))

        scheduler.reset()
        scheduler.reconcile(setOf(evening, housekeeping))

        assertEquals(setOf(morning), scheduler.cancelledMoments)
        assertEquals(setOf(evening, housekeeping), scheduler.armedMoments)
        assertEquals(setOf(evening, housekeeping), store.armed())
    }

    @Test
    fun aDeactivatedMedicineLeavesNoAlarmAtAll() = runBlocking {
        val scheduler = WatchingScheduler()
        scheduler.reconcile(setOf(morning, housekeeping))

        scheduler.reset()
        scheduler.cancel()

        assertEquals(setOf(morning, housekeeping), scheduler.cancelledMoments)
        assertTrue(scheduler.armedMoments.isEmpty())
        assertTrue("Nothing to wake up for means nothing recorded", store.armed().isEmpty())
    }

    @Test
    fun anAlarmTheRecordNeverKnewAbout_isStillArmed() = runBlocking {
        // What the watchdog meets on a phone whose alarms the platform quietly dropped: the record
        // says they are armed, the platform disagrees, and only re-arming settles it.
        store.replace(setOf(morning, evening))
        val scheduler = WatchingScheduler()

        scheduler.reconcile(setOf(morning, evening))

        assertEquals(setOf(morning, evening), scheduler.armedMoments)
    }

    @Test
    fun anAlarmForADoseAndOneForHousekeepingAtTheSameMomentAreTwoAlarms() {
        // The request code carries the kind, so the housekeeping alarm never overwrites the
        // reminder that happens to fall on the same second.
        val sameMoment = WakeMoment(morning.at, WakeKind.HOUSEKEEPING)

        assertTrue(
            ReminderAlarmScheduler.requestCode(morning) != ReminderAlarmScheduler.requestCode(sameMoment),
        )
    }

    /** A scheduler that watches the two calls that would otherwise reach `AlarmManager`. */
    private inner class WatchingScheduler : ReminderAlarmScheduler(context, store) {
        val armedMoments = mutableSetOf<WakeMoment>()
        val cancelledMoments = mutableSetOf<WakeMoment>()

        override fun setAlarm(moment: WakeMoment, exact: Boolean) {
            armedMoments += moment
        }

        override fun cancelAlarm(moment: WakeMoment) {
            cancelledMoments += moment
        }

        fun reset() {
            armedMoments.clear()
            cancelledMoments.clear()
        }
    }
}
