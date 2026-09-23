package nl.hexmaster.pillsner.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Clock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.applock.data.AndroidBiometricAvailability
import nl.hexmaster.pillsner.applock.data.DataStoreAppLockRepository
import nl.hexmaster.pillsner.applock.data.KeystorePinVerifier
import nl.hexmaster.pillsner.applock.data.LockOnBackgroundObserver
import nl.hexmaster.pillsner.applock.domain.AppLockRepository
import nl.hexmaster.pillsner.applock.domain.AppLockStateHolder
import nl.hexmaster.pillsner.applock.domain.BiometricAvailability
import nl.hexmaster.pillsner.applock.domain.ChangePin
import nl.hexmaster.pillsner.applock.domain.DisableLock
import nl.hexmaster.pillsner.applock.domain.EnablePinLock
import nl.hexmaster.pillsner.applock.domain.IsCurrentPin
import nl.hexmaster.pillsner.applock.domain.PinVerifier
import nl.hexmaster.pillsner.applock.domain.RegisterFailedAttempt
import nl.hexmaster.pillsner.applock.domain.ResolveInitialLockState
import nl.hexmaster.pillsner.applock.domain.SetBiometricUnlock
import nl.hexmaster.pillsner.applock.domain.UnlockWithPin
import nl.hexmaster.pillsner.applock.domain.VerifyIdentity
import nl.hexmaster.pillsner.applock.ui.AppLockViewModel
import nl.hexmaster.pillsner.data.RoomDoseRepository
import nl.hexmaster.pillsner.data.RoomMedicationRepository
import nl.hexmaster.pillsner.data.RoomUpcomingDosesRepository
import nl.hexmaster.pillsner.data.appinfo.BuildConfigAppInfoProvider
import nl.hexmaster.pillsner.data.stock.DataStoreStockWarningQueue
import nl.hexmaster.pillsner.data.stock.RoomStockBatchRepository
import nl.hexmaster.pillsner.data.db.PillsnerDatabase
import nl.hexmaster.pillsner.data.db.RoomTransactionRunner
import nl.hexmaster.pillsner.data.reminders.AndroidBatteryOptimisationState
import nl.hexmaster.pillsner.data.reminders.AndroidUserUnlockState
import nl.hexmaster.pillsner.data.reminders.ArmedAlarmStore
import nl.hexmaster.pillsner.data.reminders.BatteryOptimisationState
import nl.hexmaster.pillsner.data.reminders.ReminderAlarmScheduler
import nl.hexmaster.pillsner.data.reminders.ReminderCoordinator
import nl.hexmaster.pillsner.data.reminders.ReminderDeliveryLog
import nl.hexmaster.pillsner.data.reminders.TrustedClockGuard
import nl.hexmaster.pillsner.data.reminders.TrustedClockStore
import nl.hexmaster.pillsner.data.wear.DataLayerSyncTarget
import nl.hexmaster.pillsner.data.wear.DoseSyncPublisher
import nl.hexmaster.pillsner.data.wear.WearDataClientFactory
import nl.hexmaster.pillsner.data.reminders.ReminderNotifier
import nl.hexmaster.pillsner.data.reminders.ReminderPreferences
import nl.hexmaster.pillsner.data.reminders.UserUnlockState
import nl.hexmaster.pillsner.data.reminders.WakeReason
import nl.hexmaster.pillsner.data.reset.RoomAppDataEraser
import nl.hexmaster.pillsner.data.settings.DataStoreLanguageRepository
import nl.hexmaster.pillsner.data.settings.DataStoreLegalRepository
import nl.hexmaster.pillsner.data.settings.DataStoreThemeRepository
import nl.hexmaster.pillsner.domain.history.SummariseTimeDeviation
import nl.hexmaster.pillsner.domain.history.SummariseUsageHistory
import nl.hexmaster.pillsner.domain.intake.AnswerDose
import nl.hexmaster.pillsner.domain.intake.DoseAnswer
import nl.hexmaster.pillsner.domain.intake.RecordIntake
import nl.hexmaster.pillsner.domain.intake.SnoozeDose
import nl.hexmaster.pillsner.domain.legal.IsLegalAccepted
import nl.hexmaster.pillsner.domain.model.AppInfo
import nl.hexmaster.pillsner.domain.model.AppTheme
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.repository.LanguageRepository
import nl.hexmaster.pillsner.domain.repository.LegalRepository
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.repository.StockBatchRepository
import nl.hexmaster.pillsner.domain.repository.StockWarningQueue
import nl.hexmaster.pillsner.domain.repository.TransactionRunner
import nl.hexmaster.pillsner.domain.repository.ThemeRepository
import nl.hexmaster.pillsner.domain.repository.UpcomingDosesRepository
import nl.hexmaster.pillsner.domain.reset.EraseAllData
import nl.hexmaster.pillsner.domain.scheduling.ComputeWakeSchedule
import nl.hexmaster.pillsner.domain.scheduling.DoseGenerator
import nl.hexmaster.pillsner.domain.scheduling.DueDoses
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses
import nl.hexmaster.pillsner.domain.scheduling.PurgeExpiredDoseHistory
import nl.hexmaster.pillsner.domain.scheduling.RefreshPlannedDoses
import nl.hexmaster.pillsner.domain.stock.AddStockBatch
import nl.hexmaster.pillsner.domain.stock.ConsumeStockOnTaken
import nl.hexmaster.pillsner.domain.stock.EvaluateStockWarning
import nl.hexmaster.pillsner.domain.stock.ProjectWeeklyUsage
import nl.hexmaster.pillsner.ui.dose.DoseDetailViewModel
import nl.hexmaster.pillsner.ui.home.HomeViewModel
import nl.hexmaster.pillsner.ui.home.StockWarningViewModel
import nl.hexmaster.pillsner.ui.locale.AppLocale
import nl.hexmaster.pillsner.ui.medicines.QuantityFormatter
import nl.hexmaster.pillsner.ui.medicines.AmountParser
import nl.hexmaster.pillsner.ui.medicines.MedicinesViewModel
import nl.hexmaster.pillsner.ui.medicines.form.MedicationFormViewModel
import nl.hexmaster.pillsner.ui.medicines.history.MedicineHistoryViewModel
import nl.hexmaster.pillsner.ui.settings.language.LanguageSectionViewModel
import nl.hexmaster.pillsner.ui.settings.legal.LegalViewModel
import nl.hexmaster.pillsner.ui.settings.reset.ResetViewModel
import nl.hexmaster.pillsner.ui.settings.diagnostics.ReminderDiagnosticsViewModel
import nl.hexmaster.pillsner.ui.settings.theme.ThemeSectionViewModel

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
 * @param stockBatchRepository overrides the Room-backed default (`medicine-stock-tracking`).
 * @param stockWarningQueue overrides the DataStore-backed default.
 */
class AppContainer(
    context: Context,
    medicationRepository: MedicationRepository? = null,
    doseRepository: DoseRepository? = null,
    upcomingDosesRepository: UpcomingDosesRepository? = null,
    stockBatchRepository: StockBatchRepository? = null,
    stockWarningQueue: StockWarningQueue? = null,
) {
    private val applicationContext = context.applicationContext
    private val clock: Clock = Clock.systemDefaultZone()

    /** Lives as long as the process, for flows the container itself keeps hot. */
    private val containerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: PillsnerDatabase by lazy { PillsnerDatabase.build(applicationContext) }

    /** What the app says about itself on the About screen (app-about-screen design D1). */
    val appInfo: AppInfo = BuildConfigAppInfoProvider.provide(applicationContext)

    // --- Medicines and doses ---------------------------------------------------------------

    val medicationRepository: MedicationRepository =
        medicationRepository ?: RoomMedicationRepository(database.medicationDao())

    val doseRepository: DoseRepository =
        doseRepository ?: RoomDoseRepository(database.doseDao())

    val upcomingDosesRepository: UpcomingDosesRepository =
        upcomingDosesRepository ?: RoomUpcomingDosesRepository(this.doseRepository, clock)

    // --- Stock tracking (medicine-stock-tracking) -------------------------------------------

    val stockBatchRepository: StockBatchRepository =
        stockBatchRepository ?: RoomStockBatchRepository(database.stockBatchDao())

    val stockWarningQueue: StockWarningQueue =
        stockWarningQueue ?: DataStoreStockWarningQueue(applicationContext)

    /**
     * Makes several stock writes one unit (`medicine-stock-tracking`): a taken answer's intake and
     * its deduction, and a new batch with the acknowledgement it clears.
     */
    private val transactionRunner: TransactionRunner = RoomTransactionRunner(database)

    private val projectWeeklyUsage = ProjectWeeklyUsage()

    private val evaluateStockWarning = EvaluateStockWarning(
        medicationRepository = this.medicationRepository,
        stockBatchRepository = this.stockBatchRepository,
        projectWeeklyUsage = projectWeeklyUsage,
        clock = clock,
    )

    private val consumeStockOnTaken = ConsumeStockOnTaken(
        stockBatchRepository = this.stockBatchRepository,
        medicationRepository = this.medicationRepository,
        stockWarningQueue = this.stockWarningQueue,
        evaluateStockWarning = evaluateStockWarning,
    )

    private val addStockBatch = AddStockBatch(
        stockBatchRepository = this.stockBatchRepository,
        medicationRepository = this.medicationRepository,
        transactionRunner = transactionRunner,
        clock = clock,
    )

    /** Turns a medicine's stored doses into its usage history (app-medicine-usage-history D3). */
    private val summariseUsageHistory = SummariseUsageHistory(clock)

    /** Turns a medicine's taken doses into its timing accuracy (medicine-history-time-deviation D1). */
    private val summariseTimeDeviation = SummariseTimeDeviation(clock)

    // --- Reminders (app-medicine-alarm design D5, D7, D10) ---------------------------------

    private val markMissedDoses = MarkMissedDoses(this.doseRepository, clock)
    private val refreshPlannedDoses =
        RefreshPlannedDoses(this.medicationRepository, this.doseRepository, DoseGenerator(), clock)
    private val dueDoses = DueDoses(clock)
    private val computeWakeSchedule = ComputeWakeSchedule(clock)

    /** The clock-tamper guard the dose-history purge relies on (spec: dose-history-retention). */
    private val trustedClockGuard = TrustedClockGuard(TrustedClockStore(applicationContext))
    private val purgeExpiredDoseHistory = PurgeExpiredDoseHistory(clock)

    private val recordIntakeUseCase = RecordIntake(this.doseRepository, clock)
    private val snoozeDoseUseCase = SnoozeDose(this.doseRepository, markMissedDoses, clock)

    val languageRepository: LanguageRepository = DataStoreLanguageRepository(applicationContext)

    // --- Theme (app-theme-setting design D4) ------------------------------------------------

    val themeRepository: ThemeRepository = DataStoreThemeRepository(applicationContext)

    /**
     * The stored theme, correct from the first frame.
     *
     * The blocking read is the trade `PillsnerApplication.applyStoredLanguage` already makes, on
     * the same small file, which that read has pulled into DataStore's cache: microseconds against
     * the ten-second budget in this class's KDoc. Without it the first frames paint in whichever
     * scheme the default guessed — a white flash on an OLED phone at night, which is the thing the
     * setting exists to prevent.
     *
     * `themeRepository.observeTheme()` is collected exactly once, by the coroutine started here;
     * the blocking read waits for that same collection's first emission (via [firstTheme]) instead
     * of opening a second, independent subscription just to get a synchronous initial value
     * (design D4).
     *
     * Lazy, because a broadcast receiver that starts this process before the first unlock after a
     * reboot cannot read this file at all, and has no frame to paint either
     * (reminder-delivery-after-reboot design D4). The first read happens where it is needed, in
     * `MainActivity`, which only exists once the phone is unlocked.
     */
    val theme: StateFlow<AppTheme> by lazy {
        val themeState = MutableStateFlow(AppTheme.SYSTEM)
        val firstTheme = CompletableDeferred<Unit>()
        containerScope.launch {
            try {
                themeRepository.observeTheme().collect { value ->
                    themeState.value = value
                    if (!firstTheme.isCompleted) firstTheme.complete(Unit)
                }
            } catch (error: Throwable) {
                firstTheme.completeExceptionally(error)
                throw error
            }
        }
        runBlocking { firstTheme.await() }
        themeState
    }

    // --- Legal documents (app-legal-information design D3, D4) ------------------------------

    val legalRepository: LegalRepository = DataStoreLegalRepository(applicationContext, clock)
    private val isLegalAccepted = IsLegalAccepted(legalRepository)

    val reminderPreferences = ReminderPreferences(applicationContext)

    /** What the reminders did, kept on the device so a missed reminder leaves evidence behind. */
    val reminderDeliveryLog = ReminderDeliveryLog(applicationContext, clock)

    /**
     * Whether the phone has been unlocked since it booted (reminder-delivery-after-reboot D4).
     *
     * Read before anything that needs credential-encrypted storage, which on this path is almost
     * everything the app owns.
     */
    val userUnlockState: UserUnlockState = AndroidUserUnlockState(applicationContext)

    /** The armed alarms, mirrored where a locked boot can still read them (design D3, D8). */
    private val armedAlarmStore = ArmedAlarmStore(applicationContext)
    val reminderAlarmScheduler = ReminderAlarmScheduler(applicationContext, armedAlarmStore)
    val reminderNotifier = ReminderNotifier(applicationContext)

    /**
     * The third thing that can silently stop a reminder (design D6).
     *
     * Read rather than observed: the platform has no callback for it, so the Home screen asks
     * again on every resume, exactly as it does for the notification permission.
     */
    val batteryOptimisationState: BatteryOptimisationState =
        AndroidBatteryOptimisationState(applicationContext)

    // The watch, if there is one to talk to (app-wearable-support design D3). Amounts are written
    // out here, under the app language, because the phone is the only side that knows the units.
    private val wearSyncTarget = WearDataClientFactory.create(applicationContext)?.let(::DataLayerSyncTarget)
    private val doseSyncPublisher = DoseSyncPublisher(
        doseRepository = this.doseRepository,
        target = wearSyncTarget,
        amountText = QuantityFormatter(AppLocale.wrap(applicationContext), AppLocale.current)::format,
        languageTag = { AppLocale.current.toLanguageTag() },
        clock = clock,
    )

    val reminderCoordinator = ReminderCoordinator(
        medicationRepository = this.medicationRepository,
        doseRepository = this.doseRepository,
        refreshPlannedDoses = refreshPlannedDoses,
        markMissedDoses = markMissedDoses,
        dueDoses = dueDoses,
        computeWakeSchedule = computeWakeSchedule,
        notifier = reminderNotifier,
        scheduler = reminderAlarmScheduler,
        clock = clock,
        doseSyncPublisher = doseSyncPublisher,
        unlockState = userUnlockState,
        silentlyMissedReminders = reminderPreferences::recordSilentlyMissedReminder,
        deliveryLog = reminderDeliveryLog,
        trustedClockGuard = trustedClockGuard,
        purgeExpiredDoseHistory = purgeExpiredDoseHistory,
    )

    /**
     * The one way to answer a dose (app-welcome-screen-reminder-details design D1, D9).
     *
     * Declared after [reminderCoordinator] so the lambda closes over an initialised field. The
     * lambda is the Android half of an answer — what must follow it wherever it was given — and
     * keeping it here is what lets the use case stay in the domain layer.
     */
    private val answerDoseUseCase = AnswerDose(
        doseRepository = this.doseRepository,
        recordIntake = recordIntakeUseCase,
        snoozeDose = snoozeDoseUseCase,
        consumeStockOnTaken = consumeStockOnTaken,
        transactionRunner = transactionRunner,
        onAnswered = { dose ->
            reminderNotifier.cancel(dose)
            reminderCoordinator.onWake(WakeReason.ACTION)
        },
    )

    /** Records an answer, from the notification or from the dose detail screen. */
    suspend fun answerDose(id: DoseId, answer: DoseAnswer) = answerDoseUseCase(id, answer)

    /**
     * The one irreversible operation the app offers (app-settings-reset design D3).
     *
     * Declared after [reminderCoordinator] so the two ports close over initialised fields. Wiring
     * them here rather than in the use case is what keeps erasing the data a domain operation with
     * no Android type in it.
     */
    private val eraseAllData = EraseAllData(
        eraser = RoomAppDataEraser(database),
        teardown = reminderNotifier::cancelAll,
        history = {
            reminderPreferences.clearSilentlyMissedReminder()
            this.stockWarningQueue.clearAll()
        },
        refresh = { reminderCoordinator.requestWake(WakeReason.MEDICATIONS_CHANGED) },
    )

    // --- App lock (app-login design D9) ---------------------------------------------------

    private val appLockScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val appLockStateHolder = AppLockStateHolder()
    val appLockRepository: AppLockRepository = DataStoreAppLockRepository(applicationContext)
    val pinVerifier: PinVerifier = KeystorePinVerifier()
    val biometricAvailability: BiometricAvailability = AndroidBiometricAvailability(applicationContext)

    private val resolveInitialLockState = ResolveInitialLockState(appLockRepository, pinVerifier)
    private val registerFailedAttempt = RegisterFailedAttempt(appLockRepository, clock)
    private val enablePinLock = EnablePinLock(appLockRepository, pinVerifier)
    private val disableLock = DisableLock(appLockRepository)
    private val unlockWithPin = UnlockWithPin(appLockRepository, pinVerifier, registerFailedAttempt, clock)
    private val setBiometricUnlock = SetBiometricUnlock(appLockRepository)
    private val verifyIdentity = VerifyIdentity(appLockRepository, pinVerifier, registerFailedAttempt, clock)
    private val changePin = ChangePin(appLockRepository, pinVerifier)
    private val isCurrentPin = IsCurrentPin(appLockRepository, pinVerifier)

    /** Registered on `ProcessLifecycleOwner` by `PillsnerApplication` (design D2). */
    val lockOnBackgroundObserver = LockOnBackgroundObserver(appLockRepository, appLockStateHolder, appLockScope)

    /**
     * Cold start (app-login design D2): resolved once, before any content is composed.
     *
     * Called by `PillsnerApplication` rather than from an `init` block, because the stored lock
     * state is one more thing that cannot be read before the first unlock after a reboot
     * (reminder-delivery-after-reboot design D4).
     */
    fun resolveLockState() {
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
        initializer {
            StockWarningViewModel(
                stockWarningQueue = this@AppContainer.stockWarningQueue,
                evaluateStockWarning = evaluateStockWarning,
                medicationRepository = this@AppContainer.medicationRepository,
            )
        }
        initializer {
            DoseDetailViewModel(
                doseRepository = this@AppContainer.doseRepository,
                answerDose = answerDoseUseCase,
                savedStateHandle = createSavedStateHandle(),
                clock = clock,
            )
        }
        initializer {
            MedicinesViewModel(
                repository = this@AppContainer.medicationRepository,
                stockBatchRepository = this@AppContainer.stockBatchRepository,
                clock = clock,
            )
        }
        initializer { LanguageSectionViewModel(languageRepository, AppLocale.inEffect) }
        initializer { ThemeSectionViewModel(themeRepository) }
        initializer { LegalViewModel(legalRepository, isLegalAccepted) }
        initializer { ResetViewModel(eraseAllData) }
        initializer { ReminderDiagnosticsViewModel(reminderDeliveryLog) }
        initializer {
            MedicationFormViewModel(
                repository = this@AppContainer.medicationRepository,
                savedStateHandle = createSavedStateHandle(),
                amountParser = AmountParser(),
                clock = clock,
                stockBatchRepository = this@AppContainer.stockBatchRepository,
                addStockBatch = addStockBatch,
            )
        }
        initializer {
            MedicineHistoryViewModel(
                medicationRepository = this@AppContainer.medicationRepository,
                doseRepository = this@AppContainer.doseRepository,
                summarise = summariseUsageHistory,
                summariseTimeDeviation = summariseTimeDeviation,
                savedStateHandle = createSavedStateHandle(),
                clock = clock,
            )
        }
        initializer {
            AppLockViewModel(
                stateHolder = appLockStateHolder,
                repository = appLockRepository,
                biometricAvailability = biometricAvailability,
                enablePinLock = enablePinLock,
                disableLock = disableLock,
                unlockWithPin = unlockWithPin,
                setBiometricUnlock = setBiometricUnlock,
                verifyIdentity = verifyIdentity,
                changePin = changePin,
                isCurrentPin = isCurrentPin,
                clock = clock,
            )
        }
    }
}
