package nl.hexmaster.pillsner.data.stock

import java.time.LocalDate
import nl.hexmaster.pillsner.domain.model.MedicationId
import org.junit.Assert.assertEquals
import org.junit.Test

/** How one stock-warning queue entry is kept in its Preferences file (spec: medicine-stock-tracking). */
class PendingEntryTest {

    @Test
    fun `an entry with the drawn batch's expiry survives a round trip`() {
        val entry = PendingEntry(MedicationId(7), LocalDate.of(2026, 9, 24))

        assertEquals("7@2026-09-24", entry.encode())
        assertEquals(entry, PendingEntry.decode(entry.encode()))
    }

    @Test
    fun `an entry without a date is just the id, as an earlier build wrote it`() {
        assertEquals("7", PendingEntry(MedicationId(7), null).encode())
        assertEquals(PendingEntry(MedicationId(7), null), PendingEntry.decode("7"))
    }
}
