package nl.hexmaster.pillsner.data.reminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import nl.hexmaster.pillsner.MainActivity
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.ui.home.DayLabel
import nl.hexmaster.pillsner.ui.home.hasNotificationPermission
import nl.hexmaster.pillsner.ui.home.UpcomingDoseTimeFormatter
import nl.hexmaster.pillsner.ui.medicines.QuantityFormatter
import nl.hexmaster.pillsner.ui.theme.PillsnerNotificationColor

/**
 * Posts and takes down the reminder a user answers (design D7, design system 8.8).
 *
 * The notification stays until it is answered, because a reminder that can be dismissed into
 * silence is not a reminder. The three actions are always in the same order — took it, not yet,
 * not going to — so the gesture becomes muscle memory, and swiping the notification away counts as
 * "Not yet": only "Not going to" ends reminding.
 *
 * On the lock screen the system decides between the full notification and a public version that
 * says only that a medicine is due, without naming it.
 */
class ReminderNotifier(
    context: Context,
    private val quantityFormatter: QuantityFormatter = QuantityFormatter(context.applicationContext),
    private val timeFormatter: UpcomingDoseTimeFormatter = UpcomingDoseTimeFormatter(),
) {

    private val appContext = context.applicationContext
    private val notificationManager = NotificationManagerCompat.from(appContext)

    /**
     * Shows, or re-shows after a snooze, the reminder for [dose].
     *
     * Does nothing when the user has not allowed notifications: the Home screen's banner is what
     * tells them reminders cannot be delivered, and posting anyway would only throw.
     */
    fun show(dose: Dose, dueCount: Int) {
        if (!appContext.hasNotificationPermission()) return
        val text = reminderText(dose)
        val notification = NotificationCompat.Builder(appContext, ReminderChannels.REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_pillsner)
            .setColor(PillsnerNotificationColor)
            .setContentTitle(dose.medicationName)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            // False so a re-post after a snooze alerts again rather than appearing silently.
            .setOnlyAlertOnce(false)
            .setContentIntent(openApp())
            .setDeleteIntent(action(dose, ReminderAction.SNOOZE))
            .addAction(0, appContext.getString(R.string.reminder_action_took_it), action(dose, ReminderAction.TAKEN))
            .addAction(0, appContext.getString(R.string.reminder_action_not_yet), action(dose, ReminderAction.SNOOZE))
            .addAction(0, appContext.getString(R.string.reminder_action_not_going_to), action(dose, ReminderAction.SKIP))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion(dose))
            .setGroup(GROUP)
            .extend(
                // One tap on a watch should be the answer people give most.
                NotificationCompat.WearableExtender().setContentAction(TOOK_IT_ACTION_INDEX),
            )
            .build()

        post(dose.notificationId(), notification)
        if (dueCount > 1) post(SUMMARY_ID, summary(dueCount))
    }

    /**
     * The permission can be taken away between the check above and this call, and a reminder that
     * cannot be shown must never take the app down with it; the Home banner reports the state.
     */
    private fun post(id: Int, notification: android.app.Notification) {
        try {
            notificationManager.notify(id, notification)
        } catch (denied: SecurityException) {
            Log.d(TAG, "Reminder  not shown: ")
        }
    }

    /** Takes down the reminder for one dose, and the group summary when it was the last one. */
    fun cancel(dose: Dose, remainingDue: Int = 0) {
        notificationManager.cancel(dose.notificationId())
        if (remainingDue <= 1) notificationManager.cancel(SUMMARY_ID)
    }

    /** "Take 40 mg of your medicine 'Ibuprofen', on 08:00". */
    private fun reminderText(dose: Dose): String {
        val formatted = timeFormatter.format(dose.scheduledAt)
        val day = when (val label = formatted.day) {
            null -> null
            DayLabel.Tomorrow -> appContext.getString(R.string.day_tomorrow)
            is DayLabel.Text -> label.value
        }
        val time = if (day == null) {
            formatted.time
        } else {
            appContext.getString(R.string.reminder_time_with_day, formatted.time, day)
        }
        return appContext.getString(
            R.string.reminder_text,
            quantityFormatter.format(dose.amount),
            dose.medicationName,
            time,
        )
    }

    /** What a locked screen may show: that something is due, never what it is. */
    private fun publicVersion(dose: Dose) =
        NotificationCompat.Builder(appContext, ReminderChannels.REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_pillsner)
            .setColor(PillsnerNotificationColor)
            .setContentTitle(appContext.getString(R.string.reminder_public_title))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setContentIntent(openApp())
            .addAction(0, appContext.getString(R.string.reminder_action_took_it), action(dose, ReminderAction.TAKEN))
            .addAction(0, appContext.getString(R.string.reminder_action_not_yet), action(dose, ReminderAction.SNOOZE))
            .addAction(0, appContext.getString(R.string.reminder_action_not_going_to), action(dose, ReminderAction.SKIP))
            .setGroup(GROUP)
            .build()

    private fun summary(dueCount: Int) =
        NotificationCompat.Builder(appContext, ReminderChannels.REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_pillsner)
            .setColor(PillsnerNotificationColor)
            .setContentTitle(
                appContext.resources.getQuantityString(R.plurals.reminder_group_summary, dueCount, dueCount),
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp())
            .setGroup(GROUP)
            .setGroupSummary(true)
            .build()

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        appContext,
        OPEN_APP_REQUEST_CODE,
        Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** A request code per dose and action, so one dose's answers never overwrite another's. */
    private fun action(dose: Dose, action: ReminderAction): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        dose.notificationId() * ReminderAction.entries.size + action.ordinal,
        ReminderActionReceiver.intent(appContext, dose.id, action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val TAG = "Reminders"
        const val GROUP = "reminders"
        const val SUMMARY_ID = Int.MAX_VALUE
        const val OPEN_APP_REQUEST_CODE = 2
        const val TOOK_IT_ACTION_INDEX = 0

        /** Notification ids come from the dose id, so a dose has exactly one notification. */
        fun Dose.notificationId(): Int = id.value.toInt()
    }
}
