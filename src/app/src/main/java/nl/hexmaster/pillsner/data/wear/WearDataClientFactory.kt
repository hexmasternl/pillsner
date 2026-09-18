package nl.hexmaster.pillsner.data.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable

/**
 * The Data Layer client, or nothing (design D3).
 *
 * A phone without working Play services — a de-Googled one, or one where the services are too old
 * — cannot talk to a watch at all. That is not an error worth bothering the user with: the phone
 * app does everything else exactly as before, and the publisher simply has nowhere to publish.
 */
object WearDataClientFactory {

    fun create(context: Context): DataClient? = create(
        playServicesStatus = {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        },
        dataClientFactory = { Wearable.getDataClient(context.applicationContext) },
    )

    internal fun create(
        playServicesStatus: () -> Int,
        dataClientFactory: () -> DataClient,
    ): DataClient? {
        return runCatching {
            val status = playServicesStatus()
            if (status != ConnectionResult.SUCCESS) {
                Log.d(TAG, "No watch sync: Play services status $status")
                null
            } else {
                dataClientFactory()
            }
        }.getOrElse { error ->
            Log.d(TAG, "No watch sync: ${error::class.simpleName}")
            null
        }
    }

    private const val TAG = "WearSync"
}
