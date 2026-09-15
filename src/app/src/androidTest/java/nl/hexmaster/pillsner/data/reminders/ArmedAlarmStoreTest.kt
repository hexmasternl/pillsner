package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.domain.scheduling.WakeKind
import nl.hexmaster.pillsner.domain.scheduling.WakeMoment
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: reminder-scheduling, "Recovery after reboot and app update". */
@RunWith(AndroidJUnit4::class)
class ArmedAlarmStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = ArmedAlarmStore(context)

    private val dose = WakeMoment(Instant.parse("2026-09-14T06:00:00Z"), WakeKind.REMINDER)
    private val housekeeping = WakeMoment(Instant.parse("2026-09-14T22:05:00Z"), WakeKind.HOUSEKEEPING)

    @After
    fun tearDown() = runBlocking { store.clear() }

    @Test
    fun theArmedSetIsReadBack() = runBlocking {
        store.replace(setOf(dose, housekeeping))

        assertEquals(setOf(dose, housekeeping), store.armed())
    }

    @Test
    fun replacingKeepsOnlyTheNewSet() = runBlocking {
        store.replace(setOf(dose, housekeeping))

        store.replace(setOf(housekeeping))

        assertEquals(setOf(housekeeping), store.armed())
    }

    @Test
    fun theSetOutlivesTheObjectThatStoredIt() = runBlocking {
        store.replace(setOf(dose))

        // A new instance on the same file is what a locked boot reads.
        assertEquals(setOf(dose), ArmedAlarmStore(context).armed())
    }

    @Test
    fun clearingLeavesNothing() = runBlocking {
        store.replace(setOf(dose, housekeeping))

        store.clear()

        assertTrue("Nothing left to wake up for means nothing recorded", store.armed().isEmpty())
    }

    @Test
    fun withNothingEverArmedThereIsNothingToReadBack() = runBlocking {
        store.clear()

        assertTrue(store.armed().isEmpty())
    }

    @Test
    fun theStoreHoldsMomentsAndKindsAndNothingElse() = runBlocking {
        store.replace(setOf(dose, housekeeping))

        // Device-protected storage is readable before the user authenticates, so a medicine name,
        // an amount or even a dose id would be a privacy regression rather than a convenience.
        assertEquals(setOf("armed"), store.storedKeys())
    }
}
