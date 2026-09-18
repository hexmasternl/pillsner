package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.domain.scheduling.ClockSample
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: dose-history-retention, "Trusted-now guards the cutoff against a tampered wall clock". */
@RunWith(AndroidJUnit4::class)
class TrustedClockStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = TrustedClockStore(context)

    private val sample = ClockSample(Instant.parse("2026-09-18T08:00:00Z"), bootMillis = 123_456L)

    @After
    fun tearDown() = runBlocking { store.clear() }

    @Test
    fun withNothingEverWrittenThereIsNothingToReadBack() = runBlocking {
        assertNull(store.read())
    }

    @Test
    fun theSampleIsReadBack() = runBlocking {
        store.write(sample)

        assertEquals(sample, store.read())
    }

    @Test
    fun writingAgainReplacesTheSample() = runBlocking {
        store.write(sample)
        val later = sample.copy(wall = sample.wall.plusSeconds(3600), bootMillis = sample.bootMillis + 3_600_000)

        store.write(later)

        assertEquals(later, store.read())
    }

    @Test
    fun theSampleOutlivesTheObjectThatStoredIt() = runBlocking {
        store.write(sample)

        // A new instance on the same file is what the next wake, in a fresh process, reads.
        assertEquals(sample, TrustedClockStore(context).read())
    }
}
