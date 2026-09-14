package nl.hexmaster.pillsner.wear.data

import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import nl.hexmaster.pillsner.shared.wear.SyncedDoses
import nl.hexmaster.pillsner.shared.wear.WearSyncContract

/**
 * The list the phone last sent (design D4).
 *
 * Play services persists the data item on this device, so the first value is there the moment the
 * screen opens, phone connected or not. While the flow is collected, every replacement the phone
 * publishes arrives through the same flow.
 *
 * Emits null when there is no readable item: none has arrived yet, or the one that did comes from
 * a newer phone than this build understands.
 */
class UpcomingDosesRepository(
    private val dataClient: DataClient,
) {

    fun observe(): Flow<SyncedDoses?> = callbackFlow {
        val listener = DataClient.OnDataChangedListener { events ->
            events.forEach { event ->
                if (event.dataItem.uri.path != WearSyncContract.PATH) return@forEach
                when (event.type) {
                    DataEvent.TYPE_DELETED -> trySend(null)
                    else -> trySend(event.dataItem.toSyncedDoses())
                }
            }
            events.release()
        }

        dataClient.addListener(listener, contractUri(), DataClient.FILTER_LITERAL)
        trySend(readPersisted())

        awaitClose { dataClient.removeListener(listener) }
    }

    private suspend fun readPersisted(): SyncedDoses? = try {
        val buffer = dataClient.getDataItems(contractUri(), DataClient.FILTER_LITERAL).await()
        try {
            buffer.firstOrNull()?.freeze()?.toSyncedDoses()
        } finally {
            buffer.release()
        }
    } catch (error: Exception) {
        // No item, no Play services, or a disconnected client: the screen says it is not synced.
        Log.d(TAG, "No stored dose list: ${error::class.simpleName}")
        null
    }

    private fun contractUri(): Uri = Uri.Builder()
        .scheme(PutDataRequest.WEAR_URI_SCHEME)
        .path(WearSyncContract.PATH)
        .build()

    private fun DataItem.toSyncedDoses(): SyncedDoses? {
        val raw = DataMapItem.fromDataItem(this).dataMap.getString(WearSyncContract.KEY_JSON)
        return raw?.let(SyncedDoses::decode)
    }

    private companion object {
        const val TAG = "WearSync"
    }
}
