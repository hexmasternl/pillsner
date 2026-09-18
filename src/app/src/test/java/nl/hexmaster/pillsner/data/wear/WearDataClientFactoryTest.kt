package nl.hexmaster.pillsner.data.wear

import com.google.android.gms.common.ConnectionResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class WearDataClientFactoryTest {

    @Test
    fun `returns null when play services are unavailable`() {
        var dataClientRequested = false

        val client = WearDataClientFactory.create(
            playServicesStatus = { ConnectionResult.SERVICE_MISSING },
            dataClientFactory = {
                dataClientRequested = true
                throw AssertionError("DataClient must not be requested when services are unavailable")
            },
        )

        assertNull(client)
        assertFalse(dataClientRequested)
    }

    @Test
    fun `returns null when availability check throws`() {
        val client = WearDataClientFactory.create(
            playServicesStatus = { throw IllegalStateException("boom") },
            dataClientFactory = { throw AssertionError("DataClient must not be requested on failure") },
        )

        assertNull(client)
    }
}
