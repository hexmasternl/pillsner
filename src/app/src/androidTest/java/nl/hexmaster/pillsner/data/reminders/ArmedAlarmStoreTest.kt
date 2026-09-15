package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: reminder-scheduling, "Recovery after reboot and app update". */
@RunWith(AndroidJUnit4::class)
class ArmedAlarmStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = ArmedAlarmStore(context)

    private val moment: Instant = Instant.parse("2026-09-14T06:00:00Z")

    @After
    fun tearDown() = runBlocking { store.clear() }

    @Test
    fun anArmedMomentIsReadBack() = runBlocking {
        store.set(moment)

        assertEquals(moment, store.armedAt())
    }

    @Test
    fun aMomentOutlivesTheObjectThatStoredIt() = runBlocking {
        store.set(moment)

        // A new instance on the same file is what a locked boot reads.
        assertEquals(moment, ArmedAlarmStore(context).armedAt())
    }

    @Test
    fun cancellingClearsIt() = runBlocking {
        store.set(moment)

        store.clear()

        assertNull("Nothing left to wake up for means nothing recorded", store.armedAt())
    }

    @Test
    fun withNothingEverArmedThereIsNothingToReadBack() = runBlocking {
        store.clear()

        assertNull(store.armedAt())
    }

    @Test
    fun theStoreHoldsAMomentAndNothingElse() = runBlocking {
        store.set(moment)

        // Device-protected storage is readable before the user authenticates, so a medicine name,
        // an amount or even a dose id would be a privacy regression rather than a convenience.
        assertEquals(setOf("armed_at"), store.storedKeys())
    }
}
