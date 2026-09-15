package nl.hexmaster.pillsner.data.reset

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.applock.data.DataStoreAppLockRepository
import nl.hexmaster.pillsner.applock.data.KeystorePinVerifier
import nl.hexmaster.pillsner.applock.domain.Pin
import nl.hexmaster.pillsner.data.RoomDoseRepository
import nl.hexmaster.pillsner.data.RoomMedicationRepository
import nl.hexmaster.pillsner.data.db.PillsnerDatabase
import nl.hexmaster.pillsner.data.reminders.ArmedAlarmStore
import nl.hexmaster.pillsner.data.reminders.ReminderChannels
import nl.hexmaster.pillsner.data.reminders.ReminderCoordinator
import nl.hexmaster.pillsner.data.reminders.ReminderNotifier
import nl.hexmaster.pillsner.data.reminders.WakeReason
import nl.hexmaster.pillsner.data.settings.DataStoreLanguageRepository
import nl.hexmaster.pillsner.data.settings.DataStoreLegalRepository
import nl.hexmaster.pillsner.data.settings.DataStoreThemeRepository
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.domain.model.AppTheme
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.PlannedDose
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.reset.EraseAllData
import nl.hexmaster.pillsner.domain.scheduling.ComputeWakeSchedule
import nl.hexmaster.pillsner.domain.scheduling.DoseGenerator
import nl.hexmaster.pillsner.domain.scheduling.DueDoses
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses
import nl.hexmaster.pillsner.domain.scheduling.RefreshPlannedDoses
import nl.hexmaster.pillsner.domain.scheduling.WakeKind
import nl.hexmaster.pillsner.domain.scheduling.WakeMoment
import java.time.Clock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What a reset does to the things that hang off the data (spec: app-reset, design D3, D4).
 *
 * The erase is the easy half; these are the corrections that have to follow it — the notifications
 * come down, the alarms go, and every setting is left exactly where it was.
 */
@RunWith(AndroidJUnit4::class)
class AppResetEffectsTest {

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock: Clock = Clock.systemDefaultZone()

    private lateinit var database: PillsnerDatabase
    private lateinit var doses: RoomDoseRepository
    private lateinit var medications: RoomMedicationRepository
    private lateinit var notifier: ReminderNotifier
    private lateinit var armedAlarmStore: ArmedAlarmStore

    private val mg40 = Quantity.of("40", DoseUnit.MILLIGRAM)

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
    fun aReset_takesEveryPostedReminderDown() = runBlocking {
        val dose = seedDose()
        assertTrue("The reminder should be up first", notifier.show(dose, dueCount = 1))
        assertTrue(isShowing(dose.id))

        eraseWith(refresh = { }).invoke()

        // Dose ids restart at 1 after a reset, so a surviving notification would collide with a
        // dose that has nothing to do with it. Cancelling them all is why this step is not optional.
        assertFalse("No reminder may survive a reset", isShowing(dose.id))
        assertEquals(0, activeNotificationCount())
    }

    @Test
    fun aReset_leavesNoAlarmArmed() = runBlocking {
        seedDose()
        armedAlarmStore.replace(
            setOf(WakeMoment(Instant.now().plusSeconds(3_600), WakeKind.REMINDER)),
        )
        val scheduler = SilentScheduler()

        eraseWith(refresh = {
            runBlocking { coordinator(scheduler).onWake(WakeReason.MEDICATIONS_CHANGED) }
        }).invoke()

        assertTrue("An empty app has nothing to wake up for", armedAlarmStore.armed().isEmpty())
        assertTrue(scheduler.lastSchedule.isEmpty())
    }

    @Test
    fun aReset_leavesEverySettingExactlyWhereItWas() = runBlocking {
        val language = DataStoreLanguageRepository(context)
        val theme = DataStoreThemeRepository(context)
        val legal = DataStoreLegalRepository(context, clock)
        val appLock = DataStoreAppLockRepository(context)
        val verifier = KeystorePinVerifier()

        language.setLanguage(AppLanguage.DUTCH)
        theme.setTheme(AppTheme.DARK)
        legal.accept(disclaimerVersion = 1, termsVersion = 1)
        val pin = checkNotNull(Pin.of("1234"))
        appLock.storeCredential(verifier.create(pin))
        appLock.setBiometricEnabled(true)
        seedDose()

        eraseWith(refresh = { }).invoke()

        // The app stays configured; it is simply empty. Everything here lives in DataStore, which
        // the erase never touches — and this test is what keeps that true.
        assertEquals(AppLanguage.DUTCH, language.observeLanguage().first())
        assertEquals(AppTheme.DARK, theme.observeTheme().first())
        assertEquals(1, legal.observeAcceptance().first()?.disclaimerVersion)
        val settings = appLock.settings.first()
        assertTrue("The lock is still on", settings.enabled)
        assertTrue("Biometric unlock is still on", settings.biometricEnabled)
        assertTrue(
            "And the same PIN still unlocks",
            verifier.verify(pin, checkNotNull(settings.credential)),
        )
    }

    @Test
    fun theResetIsNotReachableFromOutsideTheApp() {
        // Task 8.6, as an assertion rather than a note. The reset lives behind a scroll, a tap, a
        // tick and a second tap on the Settings screen, and nothing else can reach it: the app
        // declares one launcher activity with no data intent filter, so there is no deep link, no
        // shortcut and no notification action that lands on it.
        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(context.packageName)
        assertEquals("nl.hexmaster.pillsner.MainActivity", launchIntent?.component?.className)
        assertEquals("Nothing is passed in on launch", null, launchIntent?.data)
    }

    // --- Fixtures -----------------------------------------------------------------------------

    private suspend fun seedDose(): Dose {
        val id = medications.add(
            NewMedication(
                name = "Ibuprofen",
                defaultDose = mg40,
                usedSince = LocalDate.now(clock),
                useUntil = null,
                prescribedBy = Prescriber.SELF,
                schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(8, 0)))),
            ),
        )
        doses.insertPlanned(listOf(PlannedDose(id, "Ibuprofen", mg40, Instant.now())))
        return doses.pending().first()
    }

    private fun eraseWith(refresh: () -> Unit) = EraseAllData(
        eraser = RoomAppDataEraser(database),
        teardown = notifier::cancelAll,
        refresh = refresh,
    )

    private fun coordinator(scheduler: SilentScheduler): ReminderCoordinator {
        val markMissed = MarkMissedDoses(doses, clock)
        return ReminderCoordinator(
            medicationRepository = medications,
            doseRepository = doses,
            refreshPlannedDoses = RefreshPlannedDoses(medications, doses, DoseGenerator(), clock),
            markMissedDoses = markMissed,
            dueDoses = DueDoses(doses, markMissed, clock),
            computeWakeSchedule = ComputeWakeSchedule(doses, medications, markMissed, clock),
            notifier = notifier,
            scheduler = scheduler,
            clock = clock,
        )
    }

    private fun isShowing(id: DoseId): Boolean =
        context.getSystemService(NotificationManager::class.java)
            .activeNotifications
            .any { it.id == id.value.toInt() }

    private fun activeNotificationCount(): Int =
        context.getSystemService(NotificationManager::class.java).activeNotifications.size

    /** Records into the real device-protected store without waking the device. */
    private inner class SilentScheduler :
        nl.hexmaster.pillsner.data.reminders.ReminderAlarmScheduler(context, armedAlarmStore) {

        var lastSchedule: Set<WakeMoment> = emptySet()

        override fun setAlarm(moment: WakeMoment, exact: Boolean) = Unit
        override fun cancelAlarm(moment: WakeMoment) = Unit

        override suspend fun reconcile(schedule: Set<WakeMoment>) {
            lastSchedule = schedule
            super.reconcile(schedule)
        }
    }

}
