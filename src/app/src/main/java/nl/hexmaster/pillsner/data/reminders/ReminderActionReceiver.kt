package nl.hexmaster.pillsner.data.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.PillsnerApplication
import nl.hexmaster.pillsner.domain.intake.DoseAnswer
import nl.hexmaster.pillsner.domain.model.DoseId

/** The three answers a reminder offers, in the order they always appear. */
enum class ReminderAction { TAKEN, SNOOZE, SKIP }

/**
 * The same three answers as the rest of the app calls them.
 *
 * Two enums rather than one because the notification's is part of its intent contract and the
 * domain's is not; they map one to one and must stay that way.
 */
internal fun ReminderAction.asAnswer(): DoseAnswer = when (this) {
    ReminderAction.TAKEN -> DoseAnswer.TAKEN
    ReminderAction.SNOOZE -> DoseAnswer.SNOOZE
    ReminderAction.SKIP -> DoseAnswer.SKIP
}

/**
 * Handles an answer given from the notification, on the phone or on a bridged watch (design D7).
 *
 * Swiping the notification away arrives here as [ReminderAction.SNOOZE]: only "Not going to" ends
 * reminding, so a dose can never be dismissed into silence by accident.
 *
 * Recording an answer means opening the database and reconciling the alarms, which on a cold start
 * is more than a receiver's ten seconds can promise. So it goes to the same short foreground
 * service the alarm wake uses, and only falls back into this receiver when the platform refuses to
 * start one (reminder-delivery-reliability design D4).
 */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val doseId = DoseId(intent.getLongExtra(EXTRA_DOSE_ID, -1L))
        if (doseId.value < 0) return
        val action = intent.getStringExtra(EXTRA_ACTION)?.let(ReminderAction::valueOf) ?: return

        if (ReminderWakeService.startAnswer(context, doseId, action)) return

        val container = (context.applicationContext as PillsnerApplication).container
        // Null when the receiver is driven directly rather than by a real broadcast, which is how
        // the instrumented tests exercise it.
        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                container.answerDose(doseId, action.asAnswer())
                Log.d(TAG, "Dose ${doseId.value} answered with $action")
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        private const val TAG = "Reminders"
        private const val EXTRA_DOSE_ID = "dose_id"
        private const val EXTRA_ACTION = "action"

        /** The broadcast one notification action sends. */
        fun intent(context: Context, doseId: DoseId, action: ReminderAction): Intent =
            Intent(context, ReminderActionReceiver::class.java)
                .putExtra(EXTRA_DOSE_ID, doseId.value)
                .putExtra(EXTRA_ACTION, action.name)
    }
}
