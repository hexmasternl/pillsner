package nl.hexmaster.pillsner.domain.labelscan

import java.time.LocalDate
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Design D4 rules 1 and 2: dose tokens, and which of them becomes the default dose. */
class DoseTokensTest {

    private val today = LocalDate.of(2026, 9, 29)
    private val interpret = InterpretLabelText()

    // --- Rule 1: tokens -------------------------------------------------------------------

    @Test
    fun `a strength token is a mass or volume unit`() {
        val tokens = extract("METOPROLOL 50 MG TABLET")

        assertEquals(listOf(Quantity.of("50", DoseUnit.MILLIGRAM)), tokens.map { it.quantity })
        assertTrue(tokens.single().isStrength)
    }

    @Test
    fun `a count token is a form unit in every language`() {
        assertEquals(Quantity.of("1", DoseUnit.TABLET), extract("take 1 tablet").single().quantity)
        assertEquals(Quantity.of("2", DoseUnit.TABLET), extract("2 tabletten").single().quantity)
        assertEquals(Quantity.of("1", DoseUnit.TABLET), extract("1 Tablette").single().quantity)
        assertEquals(Quantity.of("1", DoseUnit.TABLET), extract("1 comprimé").single().quantity)
        assertEquals(Quantity.of("1", DoseUnit.TABLET), extract("1 comprimido").single().quantity)
        assertEquals(Quantity.of("1", DoseUnit.CAPSULE), extract("1 cápsula").single().quantity)
        assertEquals(Quantity.of("1", DoseUnit.CAPSULE), extract("1 gélule").single().quantity)
        assertEquals(Quantity.of("10", DoseUnit.DROP), extract("10 gotas").single().quantity)
        assertEquals(Quantity.of("2", DoseUnit.PUFF), extract("2 Hübe").single().quantity)
        assertEquals(Quantity.of("500", DoseUnit.UNIT), extract("500 IE").single().quantity)
        assertFalse(extract("1 comprimé").single().isStrength)
    }

    @Test
    fun `strength units cover milligram, gram, microgram and millilitre in their spellings`() {
        assertEquals(Quantity.of("5", DoseUnit.MILLILITRE), extract("5 ml").single().quantity)
        assertEquals(Quantity.of("100", DoseUnit.MICROGRAM), extract("100 µg").single().quantity)
        assertEquals(Quantity.of("100", DoseUnit.MICROGRAM), extract("100 mcg").single().quantity)
        assertEquals(Quantity.of("1", DoseUnit.GRAM), extract("1 g").single().quantity)
        assertEquals(Quantity.of("250", DoseUnit.MILLIGRAM), extract("250mg").single().quantity)
    }

    @Test
    fun `decimals and fractions read as exact amounts`() {
        assertEquals(Quantity.of("0.5", DoseUnit.TABLET), extract("0,5 tablet").single().quantity)
        assertEquals(Quantity.of("2.5", DoseUnit.MILLILITRE), extract("2.5 ml").single().quantity)
        assertEquals(Quantity.of("0.5", DoseUnit.TABLET), extract("1/2 tablet").single().quantity)
        assertEquals(Quantity.of("0.25", DoseUnit.TABLET), extract("1/4 tablet").single().quantity)
        assertEquals(Quantity.of("0.5", DoseUnit.TABLET), extract("½ tablet").single().quantity)
    }

    @Test
    fun `half a tablet is a count of one half in every language`() {
        assertEquals(Quantity.of("0.5", DoseUnit.TABLET), extract("Take half a tablet").single().quantity)
        assertEquals(Quantity.of("0.5", DoseUnit.TABLET), extract("een halve tablet").single().quantity)
        assertEquals(Quantity.of("0.5", DoseUnit.TABLET), extract("eine halbe Tablette").single().quantity)
        assertEquals(Quantity.of("0.5", DoseUnit.TABLET), extract("un demi comprimé").single().quantity)
        assertEquals(Quantity.of("0.5", DoseUnit.TABLET), extract("media pastilla").single().quantity)
        assertEquals(Quantity.of("0.5", DoseUnit.TABLET), extract("meio comprimido").single().quantity)
        assertTrue(extract("half mg").isEmpty())
    }

    @Test
    fun `half a tablet once daily schedules half a tablet, not the label's strength`() {
        val result = interpret(lines("ZORVALEX 500 MG", "Take half a tablet once daily"), today)

        assertEquals(Quantity.of("500", DoseUnit.MILLIGRAM), result.defaultDose)
        assertEquals(Quantity.of("0.5", DoseUnit.TABLET), result.schedules.single().amount)
    }

    @Test
    fun `a range such as 1 to 2 tablets produces no schedule rather than the label's strength`() {
        assertTrue(interpret(lines("ZORVALEX 500 MG", "Take 1-2 tablets daily"), today).schedules.isEmpty())
        assertTrue(interpret(lines("ZORVALEX 500 MG", "1 tot 2 tabletten per dag"), today).schedules.isEmpty())
        assertTrue(interpret(lines("ZORVALEX 500 MG", "1 à 2 comprimés par jour"), today).schedules.isEmpty())
    }

    @Test
    fun `a fraction without an exact decimal is never rounded into a dose`() {
        assertTrue(extract("1/3 tablet").isEmpty())
        assertTrue(extract("2/3 tablet").isEmpty())
        assertNull(DoseTokens.parseNumber("1/0"))
    }

    @Test
    fun `a spelled-out count is a token but a spelled-out strength is not`() {
        assertEquals(Quantity.of("1", DoseUnit.TABLET), extract("take one tablet").single().quantity)
        assertEquals(Quantity.of("1", DoseUnit.TABLET), extract("un comprimé").single().quantity)
        assertEquals(Quantity.of("2", DoseUnit.TABLET), extract("zwei Tabletten").single().quantity)
        assertTrue(extract("one mg").isEmpty())
    }

    @Test
    fun `a number without a vocabulary unit is not a token`() {
        assertTrue(extract("2x daags").isEmpty())
        assertTrue(extract("om de 8 uur").isEmpty())
        assertTrue(extract("7 dagen").isEmpty())
        assertTrue(extract("27-09-2026").isEmpty())
    }

    @Test
    fun `a zero amount is not a token`() {
        assertTrue(extract("0 mg").isEmpty())
    }

    // --- Rule 2: default dose ---------------------------------------------------------------

    @Test
    fun `the first strength on the label is the default dose`() {
        val result = interpret(lines("PARACETAMOL 500 MG", "2x daags 1 tablet", "20 tabletten"), today)

        assertEquals(Quantity.of("500", DoseUnit.MILLIGRAM), result.defaultDose)
    }

    @Test
    fun `without a strength the count on the instruction line is the default dose, per language`() {
        assertEquals(Quantity.of("1", DoseUnit.TABLET), interpret(lines("Zorvalex", "Take 1 tablet twice daily"), today).defaultDose)
        assertEquals(Quantity.of("1", DoseUnit.TABLET), interpret(lines("Veldoprim", "2x daags 1 tablet"), today).defaultDose)
        assertEquals(Quantity.of("1", DoseUnit.TABLET), interpret(lines("Norvelin", "1 Tablette 2 mal täglich"), today).defaultDose)
        assertEquals(Quantity.of("1", DoseUnit.TABLET), interpret(lines("Calmirex", "1 comprimé 2 fois par jour"), today).defaultDose)
        assertEquals(Quantity.of("1", DoseUnit.TABLET), interpret(lines("Dolvatrex", "1 comprimido cada 8 horas"), today).defaultDose)
        assertEquals(Quantity.of("1", DoseUnit.TABLET), interpret(lines("Lumivan", "1 comprimido 2 vezes ao dia"), today).defaultDose)
    }

    @Test
    fun `a pack size on a line without a frequency is never the default dose`() {
        val result = interpret(lines("Zorvalex", "30 tabletten"), today)

        assertNull(result.defaultDose)
    }

    @Test
    fun `with neither strength nor instruction count the default dose is absent`() {
        assertNull(interpret(lines("Zorvalex", "twice daily"), today).defaultDose)
    }

    private fun extract(text: String) = DoseTokens.extract(LabelVocabulary.normalise(text))

    private fun lines(vararg texts: String) = texts.map { RecognisedLine(it, 90f) }
}
