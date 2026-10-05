package nl.hexmaster.pillsner.data.reminders

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.PillsnerApplication
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.ui.theme.PillsnerNotificationColor

/**
 * Where the reminder work actually happens (design D4).
 *
 * A broadcast receiver gets roughly ten seconds at background process priority, and its wake lock
 * is not a durable guarantee across that. A cold start that has to open Room, run its migrations
 * and read two settings files can exceed it — and an alarm is exactly the thing that starts a dead
 * process. So the receivers became thin and the work moved here, where a real foreground service
 * holds a real wake lock for a real window.
 *
 * `shortService` is the right type: this is a few database queries, not an ongoing job. The
 * platform caps it at about three minutes and the wake's own budget is nine seconds, so the service
 * stops itself long before the cap. Below Android 14 the type is simply ignored and the same
 * service runs as an ordinary foreground service.
 *
 * The notification the platform requires sits on a low-importance channel, says only that the app
 * is checking, and is gone in a second or two.
 */
class ReminderWakeService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val work = intent?.let(::commandOf)
        // A refusal is survived, not thrown: the work below still runs, in the background, and the
        // started service keeps the process alive far longer than the wake's nine seconds
        // (fix-boot-wake-service-crash design D2).
        promoteToForeground(
            promote = ::goForeground,
            log = (applicationContext as PillsnerApplication).container.reminderDeliveryLog,
            detail = work?.logDetail,
        )
        if (work == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        scope.launch {
            try {
                run(work)
            } finally {
                stopSelf(startId)
            }
        }
        // Nothing to resume if the process dies: the alarms are what bring the app back.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun run(work: Command) {
        val container = (applicationContext as PillsnerApplication).container
        when (work) {
            // The service has minutes where a receiver has seconds; say so, or the wake cancels
            // itself long before the platform would have (design D4).
            is Command.Wake -> container.reminderCoordinator.onWake(
                work.reason,
                ReminderCoordinator.SERVICE_WAKE_TIMEOUT_MILLIS,
            )

            is Command.Answer -> {
                val doseId = work.doseId
                val action = work.action
                // One use case for every answer, wherever it came from: it also takes the
                // notification down and runs the wake that follows
                // (app-welcome-screen-reminder-details design D1).
                container.answerDose(doseId, action.asAnswer())
                Log.d(TAG, "Dose ${doseId.value} answered with $action")
            }
        }
    }

    private fun goForeground() {
        val notification = NotificationCompat.Builder(this, ReminderChannels.WAKE)
            .setSmallIcon(R.drawable.ic_notification_pillsner)
            .setColor(PillsnerNotificationColor)
            .setContentTitle(getString(R.string.wake_notification))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /** The two things the service is ever asked to do. */
    private sealed interface Command {
        /** What the delivery log says about this command: never a medicine, an amount or a dose. */
        val logDetail: String

        data class Wake(val reason: WakeReason) : Command {
            override val logDetail: String get() = reason.name
        }

        data class Answer(val doseId: DoseId, val action: ReminderAction) : Command {
            override val logDetail: String get() = "ANSWER"
        }
    }

    private fun commandOf(intent: Intent): Command? = when (intent.action) {
        ACTION_WAKE -> intent.getStringExtra(EXTRA_REASON)
            ?.let { name -> WakeReason.entries.firstOrNull { it.name == name } }
            ?.let(Command::Wake)

        ACTION_ANSWER -> {
            val doseId = intent.getLongExtra(EXTRA_DOSE_ID, -1L)
            val action = intent.getStringExtra(EXTRA_ACTION)
                ?.let { name -> ReminderAction.entries.firstOrNull { it.name == name } }
            if (doseId < 0 || action == null) null else Command.Answer(DoseId(doseId), action)
        }

        else -> null
    }

    companion object {
        private const val TAG = "Reminders"
        private const val ACTION_WAKE = "nl.hexmaster.pillsner.WAKE"
        private const val ACTION_ANSWER = "nl.hexmaster.pillsner.ANSWER"
        private const val EXTRA_REASON = "reason"
        private const val EXTRA_DOSE_ID = "dose_id"
        private const val EXTRA_ACTION = "action"
        private const val NOTIFICATION_ID = Int.MAX_VALUE - 1

        /** Runs a wake in the service. @return false when the platform refused to start it. */
        fun startWake(context: Context, reason: WakeReason): Boolean =
            start(context, Intent(context, ReminderWakeService::class.java)
                .setAction(ACTION_WAKE)
                .putExtra(EXTRA_REASON, reason.name))

        /** Records an answer in the service. @return false when the platform refused to start it. */
        fun startAnswer(context: Context, doseId: DoseId, action: ReminderAction): Boolean =
            start(context, Intent(context, ReminderWakeService::class.java)
                .setAction(ACTION_ANSWER)
                .putExtra(EXTRA_DOSE_ID, doseId.value)
                .putExtra(EXTRA_ACTION, action.name))

        private fun start(context: Context, intent: Intent): Boolean = try {
            context.startForegroundService(intent)
            true
        } catch (error: Exception) {
            // Starting a foreground service from the background is allowed for an exact alarm, a
            // notification action and the non-boot system broadcasts — but the allowance is the
            // platform's to give, and the caller has a fallback that stays inside the receiver.
            // The boot wake never comes here: from Android 15 a boot broadcast may not start this
            // service type at all (fix-boot-wake-service-crash design D1). A refusal can also come
            // later, from startForeground() inside the service; promoteToForeground handles that.
            Log.d(TAG, "Wake service refused: ${error::class.simpleName}")
            false
        }
    }
}

/**
 * Asks for foreground status and survives being refused (fix-boot-wake-service-crash design D2).
 *
 * The platform can accept `startForegroundService()` and still refuse the `startForeground()` that
 * follows: a `ForegroundServiceStartNotAllowedException` from Android 12, a `SecurityException` for
 * a missing type permission, an `IllegalStateException` for a type it will not grant. The first is
 * itself an `IllegalStateException`, so catching the two base types covers all three on every API
 * level. A refusal is recorded as [DeliveryEvent.SERVICE_REFUSED] with [detail]; anything else is a
 * bug and is thrown on.
 *
 * @return whether the service is now in the foreground.
 */
internal fun promoteToForeground(promote: () -> Unit, log: ReminderDeliveryLog, detail: String?): Boolean =
    try {
        promote()
        true
    } catch (refusal: IllegalStateException) {
        recordRefusal(refusal, log, detail)
    } catch (refusal: SecurityException) {
        recordRefusal(refusal, log, detail)
    }

private fun recordRefusal(error: RuntimeException, log: ReminderDeliveryLog, detail: String?): Boolean {
    Log.d("Reminders", "Foreground refused: ${error::class.simpleName}")
    log.record(DeliveryEvent.SERVICE_REFUSED, detail)
    return false
}
