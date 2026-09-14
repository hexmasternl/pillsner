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

    fun create(context: Context) {
        val channel = NotificationChannel(
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
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}
