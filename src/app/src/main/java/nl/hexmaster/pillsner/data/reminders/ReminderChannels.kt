package nl.hexmaster.pillsner.data.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import nl.hexmaster.pillsner.R

/** The notification channel reminders are posted on. Created once, at app start. */
object ReminderChannels {

    /** The one channel: a dose being due is the only thing Pillsner interrupts anyone for. */
    const val REMINDERS = "reminders"

    /**
     * The channel the wake service's own notification sits on (design D4).
     *
     * The platform demands a notification from a foreground service; the user did not ask for one.
     * So it is as quiet as a notification can be — no sound, no vibration, no badge, no shade
     * ordering — and it is gone within a second or two of appearing.
     */
    const val WAKE = "wake"

    fun create(context: Context) {
        val reminders = NotificationChannel(
            REMINDERS,
            context.getString(R.string.reminder_channel_name),
            // A dose being due has to break through; the whole product promise rests on it.
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
            enableVibration(true)
            enableLights(true)
            setShowBadge(true)
        }

        val wake = NotificationChannel(
            WAKE,
            context.getString(R.string.wake_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = context.getString(R.string.wake_channel_description)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }

        NotificationManagerCompat.from(context).createNotificationChannels(listOf(reminders, wake))
    }
}
