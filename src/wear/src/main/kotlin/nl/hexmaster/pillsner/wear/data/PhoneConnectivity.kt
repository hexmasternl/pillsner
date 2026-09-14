package nl.hexmaster.pillsner.wear.data

import android.util.Log
import com.google.android.gms.wearable.NodeClient
import kotlinx.coroutines.tasks.await

/**
 * Whether a phone is reachable right now (design D4). Asked when the screen appears and on every
 * minute tick, so a list that can no longer be refreshed says so rather than looking live.
 */
class PhoneConnectivity(
    private val nodeClient: NodeClient,
) {
    suspend fun isPhoneConnected(): Boolean = try {
        nodeClient.connectedNodes.await().isNotEmpty()
    } catch (error: Exception) {
        Log.d(TAG, "Could not read connected nodes: ${error::class.simpleName}")
        false
    }

    private companion object {
        const val TAG = "WearSync"
    }
}
