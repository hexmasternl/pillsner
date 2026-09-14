package nl.hexmaster.pillsner

import android.app.Application
import android.content.res.Configuration
import android.content.res.Resources
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.reminders.ReminderChannels
import nl.hexmaster.pillsner.data.reminders.WakeReason
import nl.hexmaster.pillsner.di.AppContainer
import nl.hexmaster.pillsner.ui.locale.AppLocale

/** Owns the [AppContainer] for the life of the process. */
class PillsnerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Before anything reads a string or formats a date (app-settings-language design D3). One
        // small file, read once per process, so the block is measured in microseconds; everything
        // after it — every screen, every notification, every date and every name sort — depends on
        // the answer being in place.
        applyStoredLanguage()
        // Relocks the app as soon as it leaves the foreground (app-login design D2).
        ProcessLifecycleOwner.get().lifecycle.addObserver(container.lockOnBackgroundObserver)

        // Reminders (app-medicine-alarm design D5): the channel has to exist before anything is
        // posted on it, and one wake at start catches up on everything missed while the process
        // was gone.
        ReminderChannels.create(this)
        container.reminderCoordinator.start()
        container.reminderCoordinator.requestWake(WakeReason.APP_START)
    }

    /**
     * The phone's own language can change while the app is running. When the user follows the
     * phone, the app follows it too; when they picked a language, it stays.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyStoredLanguage()
    }

    private fun applyStoredLanguage() {
        val stored = runBlocking { container.languageRepository.observeLanguage().first() }
        AppLocale.apply(stored, Resources.getSystem().configuration.locales)
    }
}
