package nl.hexmaster.pillsner.data.wear

import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.shared.wear.SyncedDose
import nl.hexmaster.pillsner.shared.wear.SyncedDoses
import nl.hexmaster.pillsner.shared.wear.WearSyncContract

/** Where a published list goes. One method, so a test can stand in for the whole Data Layer. */
fun interface SyncTarget {
    suspend fun publish(json: String)
}

/** The real one: one data item at the contract path, replaced whole (design D3). */
class DataLayerSyncTarget(private val dataClient: DataClient) : SyncTarget {
    override suspend fun publish(json: String) {
        val request = PutDataMapRequest.create(WearSyncContract.PATH)
            .apply { dataMap.putString(WearSyncContract.KEY_JSON, json) }
            .asPutDataRequest()
            // A dose list the watch shows minutes late is a dose list that is wrong.
            .setUrgent()
        dataClient.putDataItem(request)
    }
}

/**
 * Tells the watch what is still to be taken (design D3).
 *
 * It publishes every pending dose the phone holds — the rolling two-day window — not just the six
 * hours the watch shows, so the watch keeps a correct list while the phone is out of reach and the
 * phone needs no timer of its own as the window slides.
 *
 * Amounts are written out here, in the phone app's language, because the phone is the only side
 * that knows the units and their translations. The language travels with them so the watch reads
 * its own strings the same way.
 *
 * Nothing to publish to is a normal state, not a failure: a phone without Play services simply has
 * no watch to talk to, and every log line here counts doses without naming one.
 */
class DoseSyncPublisher(
    private val doseRepository: DoseRepository,
    private val target: SyncTarget?,
    private val amountText: (Quantity) -> String,
    private val languageTag: () -> String,
    private val clock: Clock,
    private val debounceMillis: Long = DEBOUNCE_MILLIS,
) {

    /**
     * Publishes on every change to the pending doses. Debounced, because one wake writes several
     * rows — a missed dose, a refreshed plan, a recorded intake — and the watch only needs the
     * result.
     */
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        if (target == null) return
        scope.launch {
            doseRepository.observePending().debounce(debounceMillis).collect { doses ->
                publish(doses)
            }
        }
    }

    /**
     * Publishes the current list straight away. Used after a wake and after a language change,
     * where nothing about the doses has changed but what the watch should show has.
     */
    suspend fun publishNow() {
        if (target == null) return
        publish(doseRepository.observePending().first())
    }

    private suspend fun publish(doses: List<Dose>) {
        val payload = SyncedDoses(
            languageTag = languageTag(),
            // Always different, so the Data Layer treats it as a change and syncs it even when the
            // doses are identical — which is how a language change reaches the watch.
            publishedAtEpochMillis = Instant.now(clock).toEpochMilli(),
            doses = doses.sortedBy { it.scheduledAt }.map { it.toSyncedDose() },
        )
        try {
            target?.publish(SyncedDoses.encode(payload))
            Log.d(TAG, "Published ${payload.doses.size} doses to the watch")
        } catch (error: Exception) {
            // A watch that is not there yet is the usual case; the next change publishes again.
            Log.d(TAG, "Watch publish failed: ${error::class.simpleName}")
        }
    }

    private fun Dose.toSyncedDose() = SyncedDose(
        doseId = id.value,
        medicationName = medicationName,
        amountText = amountText(amount),
        scheduledAtEpochMillis = scheduledAt.toEpochMilli(),
    )

    private companion object {
        const val TAG = "WearSync"

        /** One wake writes several rows; the watch only needs what they add up to. */
        const val DEBOUNCE_MILLIS = 500L
    }
}
