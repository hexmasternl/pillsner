package nl.hexmaster.pillsner.domain.labelscan

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Design D4 rules 6 and 7: used since and use until, read conservatively. */
class LabelDatesTest {

    private val today = LocalDate.of(2026, 9, 29)
    private val interpret = InterpretLabelText()

    // --- Rule 6: used since -----------------------------------------------------------------

    @Test
    fun `the dispense date becomes used since and the expiry month is ignored`() {
        val result = interpret(lines("ZORVALEX 50 MG", "27-09-2026", "EXP 03/2028"), today)

        assertEquals(LocalDate.of(2026, 9, 27), result.usedSince)
        assertNull(result.useUntil)
    }

    @Test
    fun `a date on an expiry or lot line is neither a start nor an end of use`() {
        // A recent expiry date, alone on the label: still not the start of use.
        assertEquals(today, interpret(lines("ZORVALEX 50 MG", "EXP 15-09-2026"), today).usedSince)
        // "houdbaar tot" carries an until-word, but it is when the medicine goes off, not when its use ends.
        val result = interpret(lines("ZORVALEX 50 MG", "27-09-2026", "Ten minste houdbaar tot 15-10-2027"), today)
        assertEquals(LocalDate.of(2026, 9, 27), result.usedSince)
        assertNull(result.useUntil)
        assertNull(interpret(lines("ZORVALEX 50 MG", "À utiliser avant 15/10/2027"), today).useUntil)
        assertNull(interpret(lines("ZORVALEX 50 MG", "Lot A1B2 · 15-10-2027"), today).useUntil)
        assertEquals(today, interpret(lines("ZORVALEX 50 MG", "Verwendbar bis 15.09.2026"), today).usedSince)
    }

    @Test
    fun `without a date used since is today`() {
        assertEquals(today, interpret(lines("ZORVALEX 50 MG", "2x daags 1 tablet"), today).usedSince)
    }

    @Test
    fun `a date more than a year old is ignored`() {
        assertEquals(today, interpret(lines("ZORVALEX 50 MG", "01-03-2025"), today).usedSince)
    }

    @Test
    fun `a date after today is never used since`() {
        val result = interpret(lines("ZORVALEX 50 MG", "15-10-2026"), today)

        assertEquals(today, result.usedSince)
        assertNull(result.useUntil)
    }

    @Test
    fun `the most recent qualifying date wins`() {
        assertEquals(LocalDate.of(2026, 9, 27), interpret(lines("01-09-2026", "27-09-2026", "ZORVALEX 50 MG"), today).usedSince)
    }

    @Test
    fun `every numeric shape is recognised, including two-digit years`() {
        assertEquals(LocalDate.of(2026, 9, 27), interpret(lines("27/09/2026"), today).usedSince)
        assertEquals(LocalDate.of(2026, 9, 27), interpret(lines("27.09.2026"), today).usedSince)
        assertEquals(LocalDate.of(2026, 9, 27), interpret(lines("2026-09-27"), today).usedSince)
        assertEquals(LocalDate.of(2026, 9, 27), interpret(lines("27-09-26"), today).usedSince)
        assertEquals(today, interpret(lines("29-09-2026"), today).usedSince)
    }

    @Test
    fun `an impossible date is not a candidate`() {
        assertEquals(today, interpret(lines("31-02-2026"), today).usedSince)
    }

    @Test
    fun `a date that follows an until word is not used since even when it is in the past`() {
        val result = interpret(lines("ZORVALEX 50 MG", "gebruiken tot 15-09-2026"), today)

        assertEquals(today, result.usedSince)
        assertNull(result.useUntil)
    }

    // --- Rule 7: use until ------------------------------------------------------------------

    @Test
    fun `a seven-day course in Dutch ends on the seventh day inclusive`() {
        val result = interpret(lines("ZORVALEX 50 MG", "2x daags 1 tablet gedurende 7 dagen"), today)

        assertEquals(today, result.usedSince)
        assertEquals(LocalDate.of(2026, 10, 5), result.useUntil)
    }

    @Test
    fun `a two-week course in Portuguese`() {
        val result = interpret(lines("LUMIVAN 10 mg", "1 comprimido 2 vezes ao dia durante 2 semanas"), today)

        assertEquals(LocalDate.of(2026, 10, 12), result.useUntil)
    }

    @Test
    fun `an explicit until date`() {
        val result = interpret(lines("ZORVALEX 50 MG", "tot 15-10-2026"), today)

        assertEquals(LocalDate.of(2026, 10, 15), result.useUntil)
    }

    @Test
    fun `until words in every language`() {
        assertEquals(LocalDate.of(2026, 10, 15), interpret(lines("until 15/10/2026"), today).useUntil)
        assertEquals(LocalDate.of(2026, 10, 15), interpret(lines("bis zum 15.10.2026"), today).useUntil)
        assertEquals(LocalDate.of(2026, 10, 15), interpret(lines("jusqu'au 15/10/2026"), today).useUntil)
        assertEquals(LocalDate.of(2026, 10, 15), interpret(lines("hasta el 15/10/2026"), today).useUntil)
        assertEquals(LocalDate.of(2026, 10, 15), interpret(lines("até 15/10/2026"), today).useUntil)
        assertEquals(LocalDate.of(2026, 10, 15), interpret(lines("t/m 15-10-2026"), today).useUntil)
    }

    @Test
    fun `an until date wins over a course length`() {
        val result = interpret(lines("ZORVALEX 50 MG", "gedurende 7 dagen, tot 20-10-2026"), today)

        assertEquals(LocalDate.of(2026, 10, 20), result.useUntil)
    }

    @Test
    fun `a course is counted from the dispense date, not from today`() {
        val result = interpret(lines("ZORVALEX 50 MG", "27-09-2026", "for 10 days"), today)

        assertEquals(LocalDate.of(2026, 9, 27), result.usedSince)
        assertEquals(LocalDate.of(2026, 10, 6), result.useUntil)
    }

    @Test
    fun `a course that crosses a month end and one that crosses a year end`() {
        assertEquals(LocalDate.of(2026, 10, 3), interpret(lines("for 5 days"), today).useUntil)
        assertEquals(LocalDate.of(2027, 1, 5), interpret(lines("for 7 days"), LocalDate.of(2026, 12, 30)).useUntil)
    }

    @Test
    fun `German puts lang after the length`() {
        assertEquals(LocalDate.of(2026, 10, 5), interpret(lines("1-0-1 Tabletten 7 Tage lang"), today).useUntil)
    }

    @Test
    fun `duration words in every language`() {
        assertEquals(LocalDate.of(2026, 10, 5), interpret(lines("pendant 7 jours"), today).useUntil)
        assertEquals(LocalDate.of(2026, 10, 5), interpret(lines("durante 7 días"), today).useUntil)
        assertEquals(LocalDate.of(2026, 10, 12), interpret(lines("für zwei Wochen"), today).useUntil)
        assertEquals(LocalDate.of(2026, 10, 5), interpret(lines("for one week"), today).useUntil)
    }

    @Test
    fun `a bare length counts only on an instruction line`() {
        assertEquals(LocalDate.of(2026, 10, 5), interpret(lines("2x daags 1 tablet 7 dagen"), today).useUntil)
        assertNull(interpret(lines("ZORVALEX 50 MG", "7 dagen"), today).useUntil)
    }

    @Test
    fun `a rhythm such as every two days is not a two-day course`() {
        assertNull(interpret(lines("ZORVALEX 50 MG", "alle 2 Tage 1 Tablette"), today).useUntil)
    }

    @Test
    fun `an unsupported per-N-days rhythm invents no end date`() {
        val result = interpret(lines("ZORVALEX 50 MG", "1 tablet per 7 days"), today)

        assertNull(result.useUntil)
        assertTrue(result.schedules.isEmpty())
        // Spanish and Portuguese "por" is a course, and stays one.
        assertEquals(LocalDate.of(2026, 10, 5), interpret(lines("1 comprimido al día por 7 días"), today).useUntil)
    }

    private fun lines(vararg texts: String) = texts.map { RecognisedLine(it, 90f) }
}
