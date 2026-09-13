package nl.hexmaster.pillsner.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.applock.data.AndroidBiometricAvailability
import nl.hexmaster.pillsner.applock.data.DataStoreAppLockRepository
import nl.hexmaster.pillsner.applock.data.KeystorePinVerifier
import nl.hexmaster.pillsner.applock.data.LockOnBackgroundObserver
import nl.hexmaster.pillsner.applock.domain.AppLockRepository
import nl.hexmaster.pillsner.applock.domain.AppLockStateHolder
import nl.hexmaster.pillsner.applock.domain.BiometricAvailability
import nl.hexmaster.pillsner.applock.domain.DisablePinLock
import nl.hexmaster.pillsner.applock.domain.EnablePinLock
import nl.hexmaster.pillsner.applock.domain.PinVerifier
import nl.hexmaster.pillsner.applock.domain.RegisterFailedAttempt
import nl.hexmaster.pillsner.applock.domain.ResetLockAfterRecovery
import nl.hexmaster.pillsner.applock.domain.ResolveInitialLockState
import nl.hexmaster.pillsner.applock.domain.SetBiometricUnlock
import nl.hexmaster.pillsner.applock.domain.UnlockWithPin
import nl.hexmaster.pillsner.applock.ui.AppLockViewModel
import nl.hexmaster.pillsner.data.RoomDoseRepository
import nl.hexmaster.pillsner.data.RoomMedicationRepository
import nl.hexmaster.pillsner.data.RoomUpcomingDosesRepository
import nl.hexmaster.pillsner.data.db.PillsnerDatabase
import nl.hexmaster.pillsner.data.reminders.ReminderAlarmScheduler
import nl.hexmaster.pillsner.data.reminders.ReminderCoordinator
import nl.hexmaster.pillsner.data.reminders.ReminderNotifier
import nl.hexmaster.pillsner.data.reminders.ReminderPreferences
import nl.hexmaster.pillsner.data.settings.DataStoreLanguageRepository
import nl.hexmaster.pillsner.domain.intake.RecordIntake
import nl.hexmaster.pillsner.domain.intake.SnoozeDose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.repository.LanguageRepository
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.repository.UpcomingDosesRepository
import nl.hexmaster.pillsner.domain.scheduling.ComputeNextWake
import nl.hexmaster.pillsner.domain.scheduling.DoseGenerator
import nl.hexmaster.pillsner.domain.scheduling.DueDoses
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses
import nl.hexmaster.pillsner.domain.scheduling.RefreshPlannedDoses
import nl.hexmaster.pillsner.ui.home.HomeViewModel
import nl.hexmaster.pillsner.ui.locale.AppLocale
import nl.hexmaster.pillsner.ui.medicines.AmountParser
import nl.hexmaster.pillsner.ui.medicines.MedicinesViewModel
import nl.hexmaster.pillsner.ui.medicines.form.MedicationFormViewModel
import nl.hexmaster.pillsner.ui.settings.language.LanguageSectionViewModel

/**
 * The app's single dependency injection mechanism: manual constructor injection through one
 * container created by [nl.hexmaster.pillsner.PillsnerApplication] (design D6, and app-login D9).
 *
 * Construction stays cheap and free of side effects: a broadcast receiver may be the first thing
 * that starts the process, and it has ten seconds for everything (app-medicine-alarm design D10).
 *
 * Every later change adds its repositories here and registers its view models in [viewModelFactory].
 * Swapping in a DI framework is a separate proposal and would replace only this class.
 *
 * @param medicationRepository overrides the Room-backed default; tests and previews use it.
 * @param doseRepository overrides the Room-backed default.
 * @param upcomingDosesRepository overrides what the Home screen reads.
 */
class AppContainer(
    context: Context,
    medicationRepository: MedicationRepository? = null,
    doseRepository: DoseRepository? = null,
    upcomingDosesRepository: UpcomingDosesRepository? = null,
) {
    private val applicationContext = context.applicationContext
    private val clock: Clock = Clock.systemDefaultZone()

    private val database: PillsnerDatabase by lazy { PillsnerDatabase.build(applicationContext) }

    // --- Medicines and doses ---------------------------------------------------------------

    val medicationRepository: MedicationRepository =
        medicationRepository ?: RoomMedicationRepository(database.medicationDao())

    val doseRepository: DoseRepository =
        doseRepository ?: RoomDoseRepository(database.doseDao())

    val upcomingDosesRepository: UpcomingDosesRepository =
        upcomingDosesRepository ?: RoomUpcomingDosesRepository(this.doseRepository, clock)

    // --- Reminders (app-medicine-alarm design D5, D7, D10) ---------------------------------

    private val markMissedDoses = MarkMissedDoses(this.doseRepository, clock)
    private val refreshPlannedDoses =
        RefreshPlannedDoses(this.medicationRepository, this.doseRepository, DoseGenerator(), clock)
    private val dueDoses = DueDoses(this.doseRepository, clock)
    private val computeNextWake =
        ComputeNextWake(this.doseRepository, this.medicationRepository, markMissedDoses, clock)

    private val recordIntakeUseCase = RecordIntake(this.doseRepository, clock)
    private val snoozeDoseUseCase = SnoozeDose(this.doseRepository, markMissedDoses, clock)

    val languageRepository: LanguageRepository = DataStoreLanguageRepository(applicationContext)

    val reminderPreferences = ReminderPreferences(applicationContext)
    val reminderAlarmScheduler = ReminderAlarmScheduler(applicationContext)
    val reminderNotifier = ReminderNotifier(applicationContext)

    val reminderCoordinator = ReminderCoordinator(
        medicationRepository = this.medicationRepository,
        doseRepository = this.doseRepository,
        refreshPlannedDoses = refreshPlannedDoses,
        markMissedDoses = markMissedDoses,
        dueDoses = dueDoses,
        computeNextWake = computeNextWake,
        notifier = reminderNotifier,
        scheduler = reminderAlarmScheduler,
        clock = clock,
    )

    /** Records an answer given from a notification. */
    suspend fun recordIntake(id: DoseId, outcome: IntakeOutcome) = recordIntakeUseCase(id, outcome)

    /** Postpones a reminder, bounded by the moment the dose lapses. */
    suspend fun snoozeDose(id: DoseId) = snoozeDoseUseCase(id)

    // --- App lock (app-login design D9) ---------------------------------------------------

    private val appLockScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val appLockStateHolder = AppLockStateHolder()
    val appLockRepository: AppLockRepository = DataStoreAppLockRepository(applicationContext)
    val pinVerifier: PinVerifier = KeystorePinVerifier()
    val biometricAvailability: BiometricAvailability = AndroidBiometricAvailability(applicationContext)

    private val resolveInitialLockState = ResolveInitialLockState(appLockRepository, pinVerifier)
    private val registerFailedAttempt = RegisterFailedAttempt(appLockRepository, clock)
    private val enablePinLock = EnablePinLock(appLockRepository, pinVerifier)
    private val disablePinLock = DisablePinLock(appLockRepository, pinVerifier, registerFailedAttempt, clock)
    private val unlockWithPin = UnlockWithPin(appLockRepository, pinVerifier, registerFailedAttempt, clock)
    private val setBiometricUnlock = SetBiometricUnlock(appLockRepository)
    private val resetLockAfterRecovery = ResetLockAfterRecovery(appLockRepository)

    /** Registered on `ProcessLifecycleOwner` by `PillsnerApplication` (design D2). */
    val lockOnBackgroundObserver = LockOnBackgroundObserver(appLockRepository, appLockStateHolder, appLockScope)

    init {
        // Cold start (design D2): resolved once, before any content is composed.
        appLockScope.launch {
            appLockStateHolder.set(resolveInitialLockState())
        }
    }

    /** Creates every view model in the app from the dependencies held by this container. */
    val viewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            HomeViewModel(
                repository = this@AppContainer.upcomingDosesRepository,
                reminderReadiness = reminderAlarmScheduler.isExact,
                clock = clock,
                preferences = reminderPreferences,
            )
        }
        initializer { MedicinesViewModel(this@AppContainer.medicationRepository) }
        initializer { LanguageSectionViewModel(languageRepository, AppLocale.inEffect) }
        initializer {
            MedicationFormViewModel(
                repository = this@AppContainer.medicationRepository,
                savedStateHandle = createSavedStateHandle(),
                amountParser = AmountParser(),
                clock = clock,
            )
        }
        initializer {
            AppLockViewModel(
                stateHolder = appLockStateHolder,
                repository = appLockRepository,
                biometricAvailability = biometricAvailability,
                enablePinLock = enablePinLock,
                disablePinLock = disablePinLock,
                unlockWithPin = unlockWithPin,
                setBiometricUnlock = setBiometricUnlock,
                resetLockAfterRecovery = resetLockAfterRecovery,
                clock = clock,
            )
        }
    }
}
