package nl.hexmaster.pillsner.data.backup

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.RoomDoseRepository
import nl.hexmaster.pillsner.data.RoomMedicationRepository
import nl.hexmaster.pillsner.data.db.PillsnerDatabase
import nl.hexmaster.pillsner.data.reminders.ArmedAlarmStore
import nl.hexmaster.pillsner.data.reminders.ReminderAlarmScheduler
import nl.hexmaster.pillsner.data.reminders.ReminderChannels
import nl.hexmaster.pillsner.data.reminders.ReminderCoordinator
import nl.hexmaster.pillsner.data.reminders.ReminderNotifier
import nl.hexmaster.pillsner.data.reminders.WakeReason
import nl.hexmaster.pillsner.data.wear.DoseSyncPublisher
import nl.hexmaster.pillsner.data.wear.SyncTarget
import nl.hexmaster.pillsner.domain.backup.BackupDose
import nl.hexmaster.pillsner.domain.backup.BackupMedication
import nl.hexmaster.pillsner.domain.backup.BackupSnapshot
import nl.hexmaster.pillsner.domain.backup.ImportBackup
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Intake
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.PlannedDose
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.scheduling.ComputeWakeSchedule
import nl.hexmaster.pillsner.domain.scheduling.DoseGenerator
import nl.hexmaster.pillsner.domain.scheduling.DueDoses
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses
import nl.hexmaster.pillsner.domain.scheduling.RefreshPlannedDoses
import nl.hexmaster.pillsner.domain.scheduling.WakeKind
import nl.hexmaster.pillsner.domain.scheduling.WakeMoment
import nl.hexmaster.pillsner.shared.wear.SyncedDoses
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What an import does to the things that hang off the data (spec: data-backup-restore, "A completed
 * import reschedules reminders and re-syncs the watch").
 *
 * Wired the way `AppContainer` wires it: the same notifier teardown as a reset, and a refresh that
 * runs a real coordinator wake, whose scheduler is silenced and whose watch is a recording target.
 */
@RunWith(AndroidJUnit4::class)
class ImportBackupEffectsTest {

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock: Clock = Clock.systemDefaultZone()
    private val mg40 = Quantity.of("40", DoseUnit.MILLIGRAM)

    private lateinit var database: PillsnerDatabase
    private lateinit var doses: RoomDoseRepository
    private lateinit var medications: RoomMedicationRepository
    private lateinit var notifier: ReminderNotifier
    private lateinit var armedAlarmStore: ArmedAlarmStore
    private val watch = mutableListOf<SyncedDoses>()

    @Before
    fun setUp() {
        ReminderChannels.create(context)
        database = Room.inMemoryDatabaseBuilder(context, PillsnerDatabase::class.java).build()
        doses = RoomDoseRepository(database.doseDao())
        medications = RoomMedicationRepository(database.medicationDao())
        notifier = ReminderNotifier(context)
        armedAlarmStore = ArmedAlarmStore(context)
    }

    @After
    fun tearDown() = runBlocking {
        notifier.cancelAll()
        armedAlarmStore.clear()
        database.close()
    }

    @Test
    fun anImport_leavesOnlyTheAlarmsAndWatchStateTheImportedDataImplies() = runBlocking {
        // Before: an old medicine, its reminder on screen, its alarm armed, the watch told about it.
        val oldMoment = Instant.now().truncatedTo(ChronoUnit.HOURS).plus(2, ChronoUnit.HOURS).plusSeconds(7 * 60)
        val oldDose = seedOldDose(oldMoment)
        assertTrue(notifier.show(oldDose, dueCount = 1))
        armedAlarmStore.replace(setOf(WakeMoment(oldMoment, WakeKind.REMINDER)))
        val scheduler = SilentScheduler()
        val coordinator = coordinator(scheduler)

        importWith(coordinator).invoke(importedSnapshot())

        // Reminders: nothing from before is on screen.
        assertFalse("No reminder of the replaced data survives", isShowing(oldDose.id.value))

        // Alarms: what is armed is exactly what the imported schedules imply, and nothing older.
        val pending = doses.pending()
        assertTrue("The imported schedules planned doses", pending.isNotEmpty())
        assertTrue("Only the imported medicine is planned", pending.all { it.medicationName == "Imported medicine" })
        val armed = armedAlarmStore.armed()
        assertEquals(scheduler.lastSchedule, armed)
        assertFalse("The old dose's alarm is gone", armed.any { it.at == oldMoment })
        // Every future dose the imported schedules imply has its alarm. (A dose already due may
        // also have armed a repeat, which is the imported data's own and is fine.)
        val now = Instant.now()
        val reminderMoments = armed.filter { it.kind == WakeKind.REMINDER }.map { it.at }.toSet()
        pending.filter { it.scheduledAt.isAfter(now) }.forEach { dose ->
            assertTrue("${dose.scheduledAt} is armed", dose.scheduledAt in reminderMoments)
        }

        // Watch: the last thing it was told is the imported doses, and only those.
        val lastPublished = watch.last().doses
        assertEquals(pending.map { it.id.value }.toSet(), lastPublished.map { it.doseId }.toSet())
        assertTrue(lastPublished.none { it.medicationName == "Old medicine" })
    }

    private suspend fun seedOldDose(at: Instant): nl.hexmaster.pillsner.domain.model.Dose {
        val id = medications.add(
            NewMedication(
                name = "Old medicine",
                defaultDose = mg40,
                usedSince = LocalDate.now(clock),
                useUntil = null,
                prescribedBy = Prescriber.SELF,
                schedules = emptyList(),
            ),
        )
        doses.insertPlanned(listOf(PlannedDose(id, "Old medicine", mg40, at)), plannedAt = Instant.now())
        return doses.pending().single()
    }

    /** One medicine twice a day from today, plus yesterday's taken dose as history. */
    private fun importedSnapshot(): BackupSnapshot {
        val yesterday = Instant.now().minus(1, ChronoUnit.DAYS)
        return BackupSnapshot(
            medications = listOf(
                BackupMedication(
                    backupId = "m",
                    name = "Imported medicine",
                    defaultDose = mg40,
                    usedSince = LocalDate.now(clock).minusDays(30),
                    useUntil = null,
                    prescribedBy = Prescriber.GENERAL_PRACTITIONER,
                    schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))),
                    isActive = true,
                    lowStockAcknowledgement = null,
                ),
            ),
            stockBatches = emptyList(),
            doses = listOf(
                BackupDose(
                    medicationBackupId = "m",
                    medicationName = "Imported medicine",
                    amount = mg40,
                    scheduledAt = yesterday,
                    plannedAt = yesterday.minusSeconds(3600),
                    intake = Intake(IntakeOutcome.TAKEN, yesterday),
                    snoozedUntil = null,
                    firstRemindedAt = yesterday,
                    lastRemindedAt = yesterday,
                    reminderCount = 0,
                ),
            ),
        )
    }

    private fun importWith(coordinator: ReminderCoordinator) = ImportBackup(
        store = RoomBackupDataStore(database),
        teardown = notifier::cancelAll,
        history = { },
        refresh = { runBlocking { coordinator.onWake(WakeReason.MEDICATIONS_CHANGED) } },
    )

    private fun coordinator(scheduler: SilentScheduler) = ReminderCoordinator(
        medicationRepository = medications,
        doseRepository = doses,
        refreshPlannedDoses = RefreshPlannedDoses(medications, doses, DoseGenerator(), clock),
        markMissedDoses = MarkMissedDoses(doses, clock),
        dueDoses = DueDoses(clock),
        computeWakeSchedule = ComputeWakeSchedule(clock),
        notifier = notifier,
        scheduler = scheduler,
        clock = clock,
        doseSyncPublisher = DoseSyncPublisher(
            doseRepository = doses,
            target = SyncTarget { json -> watch += checkNotNull(SyncedDoses.decode(json)) },
            amountText = { it.toString() },
            languageTag = { "en" },
            clock = clock,
        ),
    )

    private fun isShowing(id: Long): Boolean =
        context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == id.toInt() }

    /** Records into the real device-protected store without waking the device. */
    private inner class SilentScheduler : ReminderAlarmScheduler(context, armedAlarmStore) {

        var lastSchedule: Set<WakeMoment> = emptySet()

        override fun setAlarm(moment: WakeMoment, exact: Boolean) = Unit
        override fun cancelAlarm(moment: WakeMoment) = Unit

        override suspend fun reconcile(schedule: Set<WakeMoment>) {
            lastSchedule = schedule
            super.reconcile(schedule)
        }
    }
}
