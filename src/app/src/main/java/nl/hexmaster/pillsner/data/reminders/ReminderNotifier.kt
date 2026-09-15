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
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.ui.home.DayLabel
import nl.hexmaster.pillsner.ui.locale.AppLocale
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
open class ReminderNotifier(
    context: Context,
    private val quantityFormatter: QuantityFormatter = QuantityFormatter(AppLocale.wrap(context.applicationContext)),
    private val timeFormatter: UpcomingDoseTimeFormatter = UpcomingDoseTimeFormatter(),
) {

    // A receiver has no activity to inherit a configuration from, so the notification would
    // otherwise be posted in the phone's language rather than the app's.
    private val appContext = AppLocale.wrap(context.applicationContext)
    private val notificationManager = NotificationManagerCompat.from(appContext)

    /**
     * Whether the user has allowed Pillsner to post notifications at all.
     *
     * The notifier is the one component that already has to know, so it is what the coordinator
     * asks rather than reaching for a framework type of its own (design D2). A dose that lapsed
     * un-reminded because this is false has a known cause and its own banner; only a dose that
     * lapsed un-reminded while this was true is evidence of an alarm the platform dropped.
     */
    open fun notificationsAllowed(): Boolean = appContext.hasNotificationPermission()

    /**
     * Shows, or re-shows after a snooze, the reminder for [dose].
     *
     * Does nothing when the user has not allowed notifications: the Home screen's banner is what
     * tells them reminders cannot be delivered, and posting anyway would only throw.
     *
     * @return whether the dose's own notification was actually posted. The caller records a dose as
     *   reminded only then, so a dose that could not be announced stays announceable and a user who
     *   grants the permission ten minutes later still gets their reminder (design D1).
     */
    open fun show(dose: Dose, dueCount: Int): Boolean {
        if (!appContext.hasNotificationPermission()) return false
        val text = reminderText(dose)
        val notification = NotificationCompat.Builder(appContext, ReminderChannels.REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_pillsner)
            .setColor(PillsnerNotificationColor)
            .setContentTitle(dose.medicationName)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            // False so a re-post after a snooze alerts again rather than appearing silently.
            .setOnlyAlertOnce(false)
            .setContentIntent(openApp())
            // So a due dose presents itself on a locked or busy phone rather than waiting silently
            // in the shade (design D5). Where the capability is not granted the platform ignores
            // this and the notification degrades to a heads-up, which is what happened before.
            .setFullScreenIntent(openApp(), true)
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

        val posted = post(dose.notificationId(), notification)
        // The summary is decoration. Whether it went up says nothing about whether the user was
        // told about this dose, so it never decides the answer.
        if (dueCount > 1) post(SUMMARY_ID, summary(dueCount))
        return posted
    }

    /**
     * The permission can be taken away between the check above and this call, and a reminder that
     * cannot be shown must never take the app down with it; the Home banner reports the state.
     *
     * @return whether the system accepted the notification.
     */
    private fun post(id: Int, notification: android.app.Notification): Boolean = try {
        notificationManager.notify(id, notification)
        true
    } catch (denied: SecurityException) {
        Log.d(TAG, "A reminder was not shown: the permission was withdrawn")
        false
    }

    /**
     * Takes down every reminder at once, for a reset that has just erased the doses behind them
     * (app-settings-reset design D4).
     *
     * `cancelAll()` is correct only because a reminder is the only notification Pillsner posts, so
     * "all of them" and "all the reminders" are the same set. A later change that adds a second
     * kind of notification must narrow this to the reminder group rather than leave it as it is.
     */
    open fun cancelAll() {
        notificationManager.cancelAll()
    }

    /** Takes down the reminder for one dose, and the group summary when it was the last one. */
    open fun cancel(dose: Dose, remainingDue: Int = 0) {
        cancel(dose.id, remainingDue)
    }

    /**
     * The same, by identifier, for a dose that is already gone. A dose withdrawn because the user
     * changed its schedule cannot be loaded any more, and cancelling a notification that was never
     * shown does nothing, so the caller need not know which is which.
     */
    open fun cancel(id: DoseId, remainingDue: Int = 0) {
        notificationManager.cancel(id.notificationId())
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
            .setCategory(NotificationCompat.CATEGORY_ALARM)
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
            .setCategory(NotificationCompat.CATEGORY_ALARM)
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
        fun DoseId.notificationId(): Int = value.toInt()

        fun Dose.notificationId(): Int = id.notificationId()
    }
}
