package nl.hexmaster.pillsner.domain.labelscan

import java.time.LocalDate
import java.time.LocalTime
import nl.hexmaster.pillsner.domain.labelscan.LabelVocabulary.DayPart
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Design D4 rules 3 and 4: frequency words to the app's schedule shapes. */
class LabelFrequencyTest {

    private val today = LocalDate.of(2026, 9, 29)
    private val interpret = InterpretLabelText()
    private val oneTablet = Quantity.of("1", DoseUnit.TABLET)
    private val eight = LocalTime.of(8, 0)

    // --- Detection ---------------------------------------------------------------------------

    @Test
    fun `times a day is read in six languages, with digits and with words`() {
        assertEquals(Frequency.TimesPerDay(2), detect("2x daags"))
        assertEquals(Frequency.TimesPerDay(3), detect("3 maal daags"))
        assertEquals(Frequency.TimesPerDay(2), detect("2 times a day"))
        assertEquals(Frequency.TimesPerDay(2), detect("twice daily"))
        assertEquals(Frequency.TimesPerDay(1), detect("once a day"))
        assertEquals(Frequency.TimesPerDay(3), detect("3 mal täglich"))
        assertEquals(Frequency.TimesPerDay(1), detect("einmal täglich"))
        assertEquals(Frequency.TimesPerDay(1), detect("une fois par jour"))
        assertEquals(Frequency.TimesPerDay(2), detect("2 fois par jour"))
        assertEquals(Frequency.TimesPerDay(2), detect("dos veces al día"))
        assertEquals(Frequency.TimesPerDay(3), detect("3 vezes ao dia"))
        assertEquals(Frequency.TimesPerDay(4), detect("viermaal daags"))
        assertEquals(Frequency.TimesPerDay(2), detect("2dd 1 tablet"))
        assertEquals(Frequency.TimesPerDay(1), detect("1 tablet per dag"))
        assertEquals(Frequency.TimesPerDay(1), detect("1 tablet daily"))
    }

    @Test
    fun `every N hours is read in six languages`() {
        assertEquals(Frequency.EveryNHours(8), detect("om de 8 uur"))
        assertEquals(Frequency.EveryNHours(8), detect("every 8 hours"))
        assertEquals(Frequency.EveryNHours(8), detect("alle 8 Stunden"))
        assertEquals(Frequency.EveryNHours(6), detect("toutes les 6 heures"))
        assertEquals(Frequency.EveryNHours(8), detect("cada 8 horas"))
        assertEquals(Frequency.EveryNHours(8), detect("a cada 8 horas"))
        assertEquals(Frequency.EveryNHours(8), detect("de 8 em 8 horas"))
        assertEquals(Frequency.EveryNHours(12), detect("elke twaalf uur"))
    }

    @Test
    fun `every other day is read in six languages`() {
        listOf(
            "om de dag", "every other day", "jeden zweiten Tag", "alle 2 Tage", "tous les deux jours",
            "cada dos días", "dia sim dia não", "a cada dois dias",
        ).forEach { assertEquals(it, Frequency.EveryOtherDay, detect("1 tablet $it")) }
    }

    @Test
    fun `day parts are read and win over a count on the same line`() {
        assertEquals(Frequency.DayParts(setOf(DayPart.MORNING, DayPart.EVENING)), detect("1 tablet 's morgens en 's avonds"))
        assertEquals(Frequency.DayParts(setOf(DayPart.MORNING, DayPart.NOON, DayPart.EVENING)), detect("1 comprimé matin, midi et soir"))
        assertEquals(Frequency.DayParts(setOf(DayPart.EVENING)), detect("1 Tablette abends"))
        assertEquals(Frequency.DayParts(setOf(DayPart.NIGHT)), detect("1 comprimido por la noche"))
        assertEquals(Frequency.DayParts(setOf(DayPart.MORNING, DayPart.NIGHT)), detect("1 comprimido de manhã e à noite"))
        assertEquals(Frequency.DayParts(setOf(DayPart.MORNING, DayPart.EVENING)), detect("2x daags 's morgens en 's avonds"))
    }

    @Test
    fun `box notation is read with three or four digits and never from a date`() {
        assertEquals(Frequency.BoxNotation(listOf(1, 0, 1)), detect("1-0-1"))
        assertEquals(Frequency.BoxNotation(listOf(1, 1, 1)), detect("Tabl. 1 - 1 - 1"))
        assertEquals(Frequency.BoxNotation(listOf(1, 0, 0, 1)), detect("1-0-0-1"))
        assertNull(detect("27-09-2026"))
        assertNull(detect("Tel. 030-1234567"))
    }

    @Test
    fun `lines without a frequency yield none`() {
        assertNull(detect("METOPROLOL 50 MG TABLET"))
        assertNull(detect("30 tabletten"))
        assertNull(detect("Apotheek De Linde"))
        assertNull(detect("1x per week"))
        assertNull(detect("iedere 3 dagen"))
    }

    // --- Schedules ----------------------------------------------------------------------------

    @Test
    fun `twice a day in Dutch becomes one tablet at 08 and 20`() {
        val result = interpret(lines("VELDOPRIM 25 MG", "2x daags 1 tablet"), today)

        assertEquals(listOf(Schedule.EveryNDays(oneTablet, 1, listOf(eight, LocalTime.of(20, 0)))), result.schedules)
    }

    @Test
    fun `one to four times a day use the fixed default times`() {
        assertEquals(listOf(eight), timesFor("1x daags 1 tablet"))
        assertEquals(listOf(eight, LocalTime.of(20, 0)), timesFor("2x daags 1 tablet"))
        assertEquals(listOf(eight, LocalTime.of(14, 0), LocalTime.of(20, 0)), timesFor("3x daags 1 tablet"))
        assertEquals(listOf(eight, LocalTime.of(12, 0), LocalTime.of(16, 0), LocalTime.of(20, 0)), timesFor("4x daags 1 tablet"))
    }

    @Test
    fun `more than four times a day produces no schedule`() {
        assertTrue(interpret(lines("5 times a day 1 tablet"), today).schedules.isEmpty())
        assertTrue(interpret(lines("6 times a day 1 tablet"), today).schedules.isEmpty())
        assertTrue(interpret(lines("8 times a day 1 tablet"), today).schedules.isEmpty())
        assertTrue(interpret(lines("0 times a day 1 tablet"), today).schedules.isEmpty())
    }

    @Test
    fun `every eight hours in Spanish becomes an every-N-hours schedule from 08`() {
        val result = interpret(lines("DOLVATREX 400 mg", "1 comprimido cada 8 horas"), today)

        assertEquals(listOf(Schedule.EveryNHours(oneTablet, 8, eight)), result.schedules)
    }

    @Test
    fun `every N hours outside the editor's intervals produces no schedule`() {
        assertTrue(interpret(lines("1 tablet every 5 hours"), today).schedules.isEmpty())
        assertTrue(interpret(lines("1 tablet every 7 hours"), today).schedules.isEmpty())
        assertEquals(listOf(Schedule.EveryNHours(oneTablet, 12, eight)), interpret(lines("1 tablet every 12 hours"), today).schedules)
    }

    @Test
    fun `every other day becomes an every-two-days schedule at 08`() {
        val result = interpret(lines("1 tablet om de dag"), today)

        assertEquals(listOf(Schedule.EveryNDays(oneTablet, 2, listOf(eight))), result.schedules)
    }

    @Test
    fun `day parts become one every-day schedule at the matching slots`() {
        val result = interpret(lines("CALMIREX 20 mg", "1 comprimé matin, midi et soir"), today)

        assertEquals(
            listOf(Schedule.EveryNDays(oneTablet, 1, listOf(eight, LocalTime.of(13, 0), LocalTime.of(18, 0)))),
            result.schedules,
        )
    }

    @Test
    fun `box notation with equal digits and no count token multiplies the default dose`() {
        val result = interpret(lines("NORVELIN 50 mg", "1-0-1"), today)

        assertEquals(
            listOf(Schedule.EveryNDays(Quantity.of("50", DoseUnit.MILLIGRAM), 1, listOf(eight, LocalTime.of(18, 0)))),
            result.schedules,
        )
        assertEquals(
            listOf(Schedule.EveryNDays(Quantity.of("100", DoseUnit.MILLIGRAM), 1, listOf(eight, LocalTime.of(18, 0)))),
            interpret(lines("NORVELIN 50 mg", "2-0-2"), today).schedules,
        )
    }

    @Test
    fun `box notation with differing digits gives one schedule per digit in the named unit`() {
        val result = interpret(lines("NORVELIN 50 mg", "2-0-1 Tabletten"), today)

        assertEquals(
            listOf(
                Schedule.EveryNDays(Quantity.of("2", DoseUnit.TABLET), 1, listOf(eight)),
                Schedule.EveryNDays(oneTablet, 1, listOf(LocalTime.of(18, 0))),
            ),
            result.schedules,
        )
    }

    @Test
    fun `box notation with a night slot uses 22`() {
        assertEquals(listOf(eight, LocalTime.of(22, 0)), timesFor("1-0-0-1 tablet"))
    }

    @Test
    fun `box notation with no unit and no strength produces nothing`() {
        assertTrue(interpret(lines("Zorvalex", "1-0-1"), today).schedules.isEmpty())
    }

    @Test
    fun `a frequency without any amount produces no schedule`() {
        assertTrue(interpret(lines("Zorvalex", "twice daily"), today).schedules.isEmpty())
    }

    @Test
    fun `a frequency without a count uses the default dose as the amount`() {
        val result = interpret(lines("ZORVALEX 50 MG", "twice daily"), today)

        assertEquals(
            listOf(Schedule.EveryNDays(Quantity.of("50", DoseUnit.MILLIGRAM), 1, listOf(eight, LocalTime.of(20, 0)))),
            result.schedules,
        )
    }

    @Test
    fun `a volume on the instruction line outranks a strength printed elsewhere`() {
        val result = interpret(lines("AMOXICILLINE 125 mg/5 ml", "Take 10 ml twice daily"), today)

        assertEquals(Quantity.of("125", DoseUnit.MILLIGRAM), result.defaultDose)
        assertEquals(
            listOf(Schedule.EveryNDays(Quantity.of("10", DoseUnit.MILLILITRE), 1, listOf(eight, LocalTime.of(20, 0)))),
            result.schedules,
        )
    }

    @Test
    fun `a count on the instruction line still outranks its own strength`() {
        val result = interpret(lines("ZORVALEX 50 MG", "1 tablet (50 mg) twice daily"), today)

        assertEquals(listOf(Schedule.EveryNDays(oneTablet, 1, listOf(eight, LocalTime.of(20, 0)))), result.schedules)
    }

    @Test
    fun `weekly and every-three-days rhythms produce no schedule`() {
        assertTrue(interpret(lines("ZORVALEX 50 MG", "1x per week 1 tablet"), today).schedules.isEmpty())
        assertTrue(interpret(lines("ZORVALEX 50 MG", "1 tablet iedere 3 dagen"), today).schedules.isEmpty())
    }

    private fun detect(text: String) = LabelFrequency.detect(LabelVocabulary.normalise(text))

    private fun timesFor(line: String): List<LocalTime> =
        (interpret(lines(line), today).schedules.single() as Schedule.EveryNDays).times

    private fun lines(vararg texts: String) = texts.map { RecognisedLine(it, 90f) }
}
