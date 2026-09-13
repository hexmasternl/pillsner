package nl.hexmaster.pillsner

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import nl.hexmaster.pillsner.data.reminders.ReminderChannels
import nl.hexmaster.pillsner.data.reminders.WakeReason
import nl.hexmaster.pillsner.di.AppContainer

/** Owns the [AppContainer] for the life of the process. */
class PillsnerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Relocks the app as soon as it leaves the foreground (app-login design D2).
        ProcessLifecycleOwner.get().lifecycle.addObserver(container.lockOnBackgroundObserver)

        // Reminders (app-medicine-alarm design D5): the channel has to exist before anything is
        // posted on it, and one wake at start catches up on everything missed while the process
        // was gone.
        ReminderChannels.create(this)
        container.reminderCoordinator.start()
        container.reminderCoordinator.requestWake(WakeReason.APP_START)
    }
}
