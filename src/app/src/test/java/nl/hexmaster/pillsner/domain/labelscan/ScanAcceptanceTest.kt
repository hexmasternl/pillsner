package nl.hexmaster.pillsner.domain.labelscan

import java.time.LocalDate
import java.time.LocalTime
import nl.hexmaster.pillsner.domain.labelscan.ScanAcceptance.Companion.isGood
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Design D3: the scan accepts itself on two consecutive good frames that agree on the name. */
class ScanAcceptanceTest {

    private val today = LocalDate.of(2026, 9, 29)
    private val fiftyMg = Quantity.of("50", DoseUnit.MILLIGRAM)

    @Test
    fun `two consecutive good frames with the same name accept the later one`() {
        val acceptance = ScanAcceptance()
        val first = frame("Metoprolol", dose = fiftyMg)
        val second = frame("Metoprolol", dose = fiftyMg)

        assertNull(acceptance.offer(first))
        assertSame(second, acceptance.offer(second))
        assertEquals(2, acceptance.framesSeen)
    }

    @Test
    fun `name comparison ignores case and whitespace`() {
        val acceptance = ScanAcceptance()

        assertNull(acceptance.offer(frame("METOPROLOL 50", dose = fiftyMg)))
        assertEquals("metoprolol50", acceptance.offer(frame("metoprolol50", dose = fiftyMg))?.name)
    }

    @Test
    fun `a single misread between two good frames is not accepted`() {
        val acceptance = ScanAcceptance()

        assertNull(acceptance.offer(frame("Metoprolol", dose = fiftyMg)))
        assertNull(acceptance.offer(frame("Metoprolal", dose = fiftyMg)))
        assertNull(acceptance.offer(frame("Metoprolol", dose = fiftyMg)))
        // The two readings after the misread now agree.
        assertEquals("Metoprolol", acceptance.offer(frame("Metoprolol", dose = fiftyMg))?.name)
    }

    @Test
    fun `a partial read with only a name is never good, however often it repeats`() {
        val acceptance = ScanAcceptance()

        assertNull(acceptance.offer(frame("Metoprolol")))
        assertNull(acceptance.offer(frame("Metoprolol")))
        assertNull(acceptance.offer(frame("Metoprolol")))
    }

    @Test
    fun `a schedule counts as much as a dose towards a good frame`() {
        val schedule = Schedule.EveryNDays(fiftyMg, 1, listOf(LocalTime.of(8, 0)))
        val acceptance = ScanAcceptance()

        assertNull(acceptance.offer(frame("Metoprolol", schedules = listOf(schedule))))
        assertEquals("Metoprolol", acceptance.offer(frame("Metoprolol", schedules = listOf(schedule)))?.name)
    }

    @Test
    fun `a bad frame between two good ones breaks the streak`() {
        val acceptance = ScanAcceptance()

        assertNull(acceptance.offer(frame("Metoprolol", dose = fiftyMg)))
        assertNull(acceptance.offer(LabelInterpretation.empty(today)))
        assertNull(acceptance.offer(frame("Metoprolol", dose = fiftyMg)))
    }

    @Test
    fun `reset forgets the previous frame`() {
        val acceptance = ScanAcceptance()
        acceptance.offer(frame("Metoprolol", dose = fiftyMg))

        acceptance.reset()

        assertNull(acceptance.offer(frame("Metoprolol", dose = fiftyMg)))
        assertEquals(1, acceptance.framesSeen)
    }

    @Test
    fun `isGood needs a name and either a dose or a schedule`() {
        assertTrue(frame("Metoprolol", dose = fiftyMg).isGood)
        assertFalse(frame("Metoprolol").isGood)
        assertFalse(LabelInterpretation(defaultDose = fiftyMg, usedSince = today).isGood)
        assertFalse(LabelInterpretation.empty(today).isGood)
    }

    private fun frame(name: String, dose: Quantity? = null, schedules: List<Schedule> = emptyList()) =
        LabelInterpretation(name = name, defaultDose = dose, schedules = schedules, usedSince = today)
}
