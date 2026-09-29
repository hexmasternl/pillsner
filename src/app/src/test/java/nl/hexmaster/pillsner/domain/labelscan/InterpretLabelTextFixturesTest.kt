package nl.hexmaster.pillsner.domain.labelscan

import java.time.LocalDate
import java.time.LocalTime
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One whole label per app language, end to end (design D4). The medicine names are invented and
 * the people are fictitious; none of these is a real person's label.
 */
class InterpretLabelTextFixturesTest {

    private val today = LocalDate.of(2026, 9, 29)
    private val interpret = InterpretLabelText()
    private val oneTablet = Quantity.of("1", DoseUnit.TABLET)
    private val eight = LocalTime.of(8, 0)
    private val twenty = LocalTime.of(20, 0)

    @Test
    fun english_pharmacyLabel() {
        val result = interpret(
            lines(
                "Greenfield Pharmacy",
                "Mrs A. Example",
                "Tel 020 7946 0000",
                "Dispensed 27/09/2026",
                "ZORVALEX 50 MG TABLETS",
                "Take one tablet twice daily for 10 days",
                "28 tablets",
            ),
            today,
        )

        assertEquals(
            LabelInterpretation(
                name = "ZORVALEX",
                defaultDose = Quantity.of("50", DoseUnit.MILLIGRAM),
                schedules = listOf(Schedule.EveryNDays(oneTablet, 1, listOf(eight, twenty))),
                usedSince = LocalDate.of(2026, 9, 27),
                useUntil = LocalDate.of(2026, 10, 6),
                rawText = result.rawText,
            ),
            result,
        )
        assertTrue(result.rawText.startsWith("Greenfield Pharmacy\nMrs A. Example"))
    }

    @Test
    fun dutch_pharmacyLabel() {
        val result = interpret(
            lines(
                "Apotheek De Linde",
                "Mevr. B. Voorbeeld",
                "1234 AB Utrecht",
                "Tel. 030-1234567",
                "27-09-2026",
                "VELDOPRIM 25 MG TABLET",
                "2x daags 1 tablet gedurende 7 dagen",
                "30 tabletten",
            ),
            today,
        )

        assertEquals("VELDOPRIM", result.name)
        assertEquals(Quantity.of("25", DoseUnit.MILLIGRAM), result.defaultDose)
        assertEquals(listOf(Schedule.EveryNDays(oneTablet, 1, listOf(eight, twenty))), result.schedules)
        assertEquals(LocalDate.of(2026, 9, 27), result.usedSince)
        assertEquals(LocalDate.of(2026, 10, 3), result.useUntil)
    }

    @Test
    fun german_boxWithBoxNotation() {
        val result = interpret(
            lines(
                "Apotheke am Markt",
                "Herr C. Beispiel",
                "NORVELIN 100 mg Filmtabletten",
                "1-0-1",
                "Abgabe: 25.09.2026",
                "Ch.-B.: A1B2C3",
            ),
            today,
        )

        assertEquals("NORVELIN", result.name)
        assertEquals(Quantity.of("100", DoseUnit.MILLIGRAM), result.defaultDose)
        assertEquals(
            listOf(Schedule.EveryNDays(Quantity.of("100", DoseUnit.MILLIGRAM), 1, listOf(eight, LocalTime.of(18, 0)))),
            result.schedules,
        )
        assertEquals(LocalDate.of(2026, 9, 25), result.usedSince)
        assertEquals(null, result.useUntil)
    }

    @Test
    fun french_morningAndEveningForSevenDays() {
        val result = interpret(
            lines(
                "Pharmacie du Centre",
                "Mme D. Exemple",
                "Délivré le 26/09/2026",
                "CALMIREX 20 mg comprimés pelliculés",
                "1 comprimé matin et soir pendant 7 jours",
            ),
            today,
        )

        assertEquals("CALMIREX", result.name)
        assertEquals(Quantity.of("20", DoseUnit.MILLIGRAM), result.defaultDose)
        assertEquals(listOf(Schedule.EveryNDays(oneTablet, 1, listOf(eight, LocalTime.of(18, 0)))), result.schedules)
        assertEquals(LocalDate.of(2026, 9, 26), result.usedSince)
        assertEquals(LocalDate.of(2026, 10, 2), result.useUntil)
    }

    @Test
    fun spanish_everyEightHours() {
        val result = interpret(
            lines(
                "Farmacia Central",
                "Sra. E. Ejemplo",
                "DOLVATREX 400 mg comprimidos recubiertos",
                "1 comprimido cada 8 horas",
                "Fecha: 28/09/2026",
            ),
            today,
        )

        assertEquals("DOLVATREX", result.name)
        assertEquals(Quantity.of("400", DoseUnit.MILLIGRAM), result.defaultDose)
        assertEquals(listOf(Schedule.EveryNHours(oneTablet, 8, eight)), result.schedules)
        assertEquals(LocalDate.of(2026, 9, 28), result.usedSince)
        assertEquals(null, result.useUntil)
    }

    @Test
    fun portuguese_twiceADayForTwoWeeks() {
        val result = interpret(
            lines(
                "Farmácia Boa Saúde",
                "Sr. F. Exemplo",
                "LUMIVAN 10 mg comprimidos",
                "Tomar 1 comprimido 2 vezes ao dia durante 2 semanas",
                "Data: 29/09/2026",
            ),
            today,
        )

        assertEquals("LUMIVAN", result.name)
        assertEquals(Quantity.of("10", DoseUnit.MILLIGRAM), result.defaultDose)
        assertEquals(listOf(Schedule.EveryNDays(oneTablet, 1, listOf(eight, twenty))), result.schedules)
        assertEquals(LocalDate.of(2026, 9, 29), result.usedSince)
        assertEquals(LocalDate.of(2026, 10, 12), result.useUntil)
    }

    @Test
    fun nothingUsable_isEmptyAndStartsToday() {
        val result = interpret(lines("Apotheek De Linde", "Tel. 030-1234567", "27-09-2026"), today)

        assertTrue(result.isEmpty)
        assertEquals(LocalDate.of(2026, 9, 27), result.usedSince)
        assertEquals(null, result.useUntil)
    }

    @Test
    fun noLinesAtAll_isEmpty() {
        val result = interpret(emptyList(), today)

        assertEquals(LabelInterpretation.empty(today), result)
        assertTrue(result.isEmpty)
    }

    private fun lines(vararg texts: String) = texts.map { RecognisedLine(it, 90f) }
}
