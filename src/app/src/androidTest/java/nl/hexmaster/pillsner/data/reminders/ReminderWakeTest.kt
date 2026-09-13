package nl.hexmaster.pillsner.data.reminders

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.scheduling.ComputeNextWake
import nl.hexmaster.pillsner.domain.scheduling.DoseGenerator
import nl.hexmaster.pillsner.domain.scheduling.DueDoses
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses
import nl.hexmaster.pillsner.domain.scheduling.RefreshPlannedDoses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A wake cycle end to end against the real platform: a medicine that is due gets its dose planned
 * and announced, the dose is marked as reminded, and the next alarm is set
 * (spec: reminder-scheduling).
 *
 * The repositories are in memory so the device's own database is untouched, and the alarm goes to a
 * scheduler the test can see rather than to the real one.
 */
@RunWith(AndroidJUnit4::class)
class ReminderWakeTest {

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val today: LocalDate = LocalDate.of(2026, 9, 14)

    /** Ten past nine, so the nine o'clock dose of an hourly medicine is already due. */
    private val now: Instant = ZonedDateTime.of(today, LocalTime.of(9, 10), zone).toInstant()

    private val hourly = Medication(
        id = MedicationId(1),
        name = "Ibuprofen",
        defaultDose = Quantity.of("40", DoseUnit.MILLIGRAM),
        usedSince = today,
        useUntil = null,
        prescribedBy = Prescriber.SELF,
        schedules = listOf(
            Schedule.EveryNHours(Quantity.of("40", DoseUnit.MILLIGRAM), 1, LocalTime.of(0, 0)),
        ),
        isActive = true,
    )

    @Test
    fun aWakeWithADueDose_announcesItAndSetsTheNextAlarm() = runBlocking {
        val doses = InMemoryDoseRepository()
        val scheduler = RecordingScheduler()
        val medications = InMemoryMedicationRepository(listOf(hourly))

        coordinator(doses, medications, scheduler).onWake(WakeReason.ALARM)

        val nineOClock = ZonedDateTime.of(today, LocalTime.of(9, 0), zone).toInstant()
        val announced = doses.all().first { it.scheduledAt == nineOClock }
        assertNotNull("The dose that is due should have been announced", announced.firstRemindedAt)

        val tenOClock = ZonedDateTime.of(today, LocalTime.of(10, 0), zone).toInstant()
        assertEquals("The next alarm is the next dose", tenOClock, scheduler.scheduledAt)

        doses.all().forEach { ReminderNotifier(context).cancel(it) }
    }

    @Test
    fun aDoseStillAhead_isNotAnnouncedYet() = runBlocking {
        val doses = InMemoryDoseRepository()
        val medications = InMemoryMedicationRepository(listOf(hourly))

        coordinator(doses, medications, RecordingScheduler()).onWake(WakeReason.ALARM)

        val elevenOClock = ZonedDateTime.of(today, LocalTime.of(11, 0), zone).toInstant()
        assertNull(doses.all().first { it.scheduledAt == elevenOClock }.firstRemindedAt)

        doses.all().forEach { ReminderNotifier(context).cancel(it) }
    }

    @Test
    fun aWakeWithNothingToDo_leavesNoAlarmBehind() = runBlocking {
        val scheduler = RecordingScheduler()

        coordinator(InMemoryDoseRepository(), InMemoryMedicationRepository(), scheduler)
            .onWake(WakeReason.APP_START)

        assertNull(scheduler.scheduledAt)
        assertTrue(scheduler.cancelled)
    }

    @Test
    fun theBootBroadcast_isAccepted() {
        // Proves the receiver handles the action; what it then does is covered by the wake tests.
        SystemEventsReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
    }

    private fun coordinator(
        doses: InMemoryDoseRepository,
        medications: InMemoryMedicationRepository,
        scheduler: ReminderAlarmScheduler,
    ): ReminderCoordinator {
        val clock = Clock.fixed(now, zone)
        val markMissed = MarkMissedDoses(doses, clock)
        return ReminderCoordinator(
            medicationRepository = medications,
            doseRepository = doses,
            refreshPlannedDoses = RefreshPlannedDoses(medications, doses, DoseGenerator(), clock),
            markMissedDoses = markMissed,
            dueDoses = DueDoses(doses, clock),
            computeNextWake = ComputeNextWake(doses, medications, markMissed, clock),
            notifier = ReminderNotifier(context),
            scheduler = scheduler,
            clock = clock,
        )
    }

    /** A scheduler that records what it was asked to do instead of waking the device. */
    private inner class RecordingScheduler : ReminderAlarmScheduler(context) {
        var scheduledAt: Instant? = null
        var cancelled = false

        override fun scheduleAt(at: Instant) {
            scheduledAt = at
        }

        override fun cancel() {
            cancelled = true
        }
    }
}
