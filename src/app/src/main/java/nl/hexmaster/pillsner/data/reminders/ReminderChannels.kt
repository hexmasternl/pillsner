package nl.hexmaster.pillsner.data.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationManagerCompat
import nl.hexmaster.pillsner.R

/** The notification channels reminders are posted on. Created once, at app start. */
object ReminderChannels {

    /**
     * The one channel that interrupts anyone: a dose being due.
     *
     * Its sound plays on the **alarm** stream, not the notification stream. A reminder on the
     * notification stream is silenced by Do Not Disturb — even in its default configuration, which
     * lets alarms through — and by a phone set to silent or vibrate, which is exactly where a
     * medication reminder is most likely to be missed. On the alarm stream it rings at alarm
     * volume, the way the clock app does, and Do Not Disturb treats it as an alarm because the
     * notification itself is categorised as one.
     *
     * A channel's sound and audio attributes are fixed when it is created, so this is a new channel
     * rather than a change to the old one; the old one is deleted below so it does not linger in
     * the phone's settings as a duplicate.
     */
    const val REMINDERS = "reminders_alarm"

    /** The channel reminders used to be posted on, before they rang like alarms. */
    private const val LEGACY_REMINDERS = "reminders"

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
            // The phone's own notification sound, so it is recognisable, but on the alarm stream,
            // so a silent phone and Do Not Disturb do not swallow it.
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
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

        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannels(listOf(reminders, wake))
        // Deleting a channel takes down anything posted on it. A reminder showing at the moment of
        // the upgrade comes back with its next repeat, if one is still due; otherwise the dose is
        // waiting on Home. That is the one upgrade's cost, and it is paid once.
        manager.deleteNotificationChannel(LEGACY_REMINDERS)
    }
}
