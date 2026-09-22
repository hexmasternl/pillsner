package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.domain.scheduling.TrustedNow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: dose-history-retention, "Trusted-now clock guard" — the two persisted longs. */
@RunWith(AndroidJUnit4::class)
class TrustedClockStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = TrustedClockStore(context)

    private val sample = TrustedNow.Sample(trustedNowMillis = 1_000_000L, anchorElapsedRealtimeMillis = 5_000L)

    @After
    fun tearDown() = runBlocking { store.clear() }

    @Test
    fun withNothingEverWrittenThereIsNoPriorSample() = runBlocking {
        assertNull(store.read())
    }

    @Test
    fun aWrittenSampleIsReadBack() = runBlocking {
        store.write(sample)

        assertEquals(sample, store.read())
    }

    @Test
    fun writingReplacesWhateverWasThereBefore() = runBlocking {
        store.write(sample)

        val advanced = sample.copy(trustedNowMillis = 2_000_000L, anchorElapsedRealtimeMillis = 15_000L)
        store.write(advanced)

        assertEquals(advanced, store.read())
    }

    @Test
    fun theSampleOutlivesTheObjectThatStoredIt() = runBlocking {
        store.write(sample)

        // A fresh process reads the same file, exactly as a wake started by a cold alarm would.
        assertEquals(sample, TrustedClockStore(context).read())
    }

    @Test
    fun clearingLeavesNoPriorSample() = runBlocking {
        store.write(sample)

        store.clear()

        assertNull(store.read())
    }
}
