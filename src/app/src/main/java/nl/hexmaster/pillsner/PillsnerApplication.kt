package nl.hexmaster.pillsner

import android.app.Application
import android.content.res.Configuration
import android.content.res.Resources
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.reminders.ReminderChannels
import nl.hexmaster.pillsner.data.reminders.ReminderWatchdog
import nl.hexmaster.pillsner.data.reminders.WakeReason
import nl.hexmaster.pillsner.di.AppContainer
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.ui.locale.AppLocale

/** Owns the [AppContainer] for the life of the process. */
class PillsnerApplication : Application() {

    lateinit var container: AppContainer
        private set

    private var started = false

    /**
     * The language resolved for this process, cached by [applyStoredLanguage]'s one DataStore read
     * (design D3). A stored language can only change on the next app start, so a later locale change
     * reapplies this value instead of reading it again.
     */
    private lateinit var storedLanguage: AppLanguage

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Between a reboot and the first unlock nothing below can run: the language, the theme, the
        // medicines and the doses all live in credential-encrypted storage, which is unreadable
        // until the user authenticates (reminder-delivery-after-reboot design D4). A direct-boot
        // receiver may be what started this process, and all it needs is the alarm scheduler.
        // `ACTION_USER_UNLOCKED` calls back here the moment the rest becomes possible.
        if (container.userUnlockState.isUnlocked()) startWhenUnlocked()
    }

    /**
     * Everything that needs credential-encrypted storage, once it can be read.
     *
     * Called from [onCreate] on an ordinary start, and from `SystemEventsReceiver` at the first
     * unlock after a reboot. Doing nothing the second time is the point: either route may come
     * first, and neither knows about the other.
     */
    @Synchronized
    fun startWhenUnlocked() {
        if (started) return
        started = true

        // Before anything reads a string or formats a date (app-settings-language design D3). One
        // small file, read once per process, so the block is measured in microseconds; everything
        // after it — every screen, every notification, every date and every name sort — depends on
        // the answer being in place.
        applyStoredLanguage()
        // Relocks the app as soon as it leaves the foreground (app-login design D2).
        ProcessLifecycleOwner.get().lifecycle.addObserver(container.lockOnBackgroundObserver)
        container.resolveLockState()

        // Reminders (app-medicine-alarm design D5): the channel has to exist before anything is
        // posted on it, and one wake at start catches up on everything missed while the process
        // was gone.
        ReminderChannels.create(this)
        container.reminderCoordinator.start()
        // The net under the alarms (reminder-delivery-reliability design D3). Unique work with
        // KEEP, so a phone the user opens often still completes an interval; enqueuing is a
        // handful of microseconds and does not touch the database.
        ReminderWatchdog.enqueue(this)
        container.reminderCoordinator.requestWake(WakeReason.APP_START)
    }

    /**
     * The phone's own language can change while the app is running. When the user follows the
     * phone, the app follows it too; when they picked a language, it stays.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (started) AppLocale.apply(storedLanguage, Resources.getSystem().configuration.locales)
    }

    private fun applyStoredLanguage() {
        storedLanguage = runBlocking { container.languageRepository.observeLanguage().first() }
        AppLocale.apply(storedLanguage, Resources.getSystem().configuration.locales)
    }
}
