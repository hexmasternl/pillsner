package nl.hexmaster.pillsner.wear.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import nl.hexmaster.pillsner.shared.wear.SyncedDose
import nl.hexmaster.pillsner.shared.wear.SyncedDoses
import nl.hexmaster.pillsner.shared.wear.WearSyncContract
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real Data Layer: an item written on this device is what the repository reads back.
 * The phone half of the sync is not involved — what is under test is that the watch reads the
 * contract path and decodes what it finds there, both when the screen opens and while it is open.
 */
@RunWith(AndroidJUnit4::class)
class UpcomingDosesRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dataClient = Wearable.getDataClient(context)
    private val repository = UpcomingDosesRepository(dataClient)

    private val contractUri: Uri = Uri.Builder()
        .scheme(PutDataRequest.WEAR_URI_SCHEME)
        .path(WearSyncContract.PATH)
        .build()

    /**
     * The Wearable API is only present on a Wear OS device, or on a phone paired through the Wear
     * companion app. Everywhere else these two tests have nothing to talk to, and a skip says that
     * honestly rather than a pass that proved nothing.
     */
    @Before
    fun requireTheWearableApi() = runBlocking {
        val available = runCatching { dataClient.getDataItems(contractUri).await().release() }.isSuccess
        assumeTrue("The Wearable API needs a Wear OS device or a paired phone", available)
    }

    @After
    fun removeTheItem() = runBlocking {
        runCatching { dataClient.deleteDataItems(contractUri).await() }
        Unit
    }

    private suspend fun publish(vararg names: String) {
        val payload = SyncedDoses(
            languageTag = "en",
            publishedAtEpochMillis = System.currentTimeMillis(),
            doses = names.mapIndexed { index, name ->
                SyncedDose(index.toLong(), name, "400 mg", System.currentTimeMillis())
            },
        )
        val request = PutDataMapRequest.create(WearSyncContract.PATH)
            .apply { dataMap.putString(WearSyncContract.KEY_JSON, SyncedDoses.encode(payload)) }
            .asPutDataRequest()
            .setUrgent()
        dataClient.putDataItem(request).await()
    }

    @Test
    fun theStoredListIsThereWhenTheScreenOpens() = runBlocking {
        publish("Ibuprofen", "Metformin")

        val payload = withTimeout(TIMEOUT_MILLIS) {
            repository.observe().first { it?.doses?.size == 2 }
        }

        assertEquals(listOf("Ibuprofen", "Metformin"), payload?.doses?.map { it.medicationName })
    }

    @Test
    fun aNewListArrivesWhileTheScreenIsOpen() = runBlocking {
        publish("Ibuprofen")

        val payload = withTimeout(TIMEOUT_MILLIS) {
            coroutineScope {
                val arriving = async {
                    repository.observe().first { it?.doses?.singleOrNull()?.medicationName == "Metformin" }
                }
                // Let the listener attach first, so this is the update path and not the first read.
                delay(SETTLE_MILLIS)
                publish("Metformin")
                arriving.await()
            }
        }

        assertEquals(listOf("Metformin"), payload?.doses?.map { it.medicationName })
    }

    private companion object {
        const val TIMEOUT_MILLIS = 20_000L
        const val SETTLE_MILLIS = 1_000L
    }
}
