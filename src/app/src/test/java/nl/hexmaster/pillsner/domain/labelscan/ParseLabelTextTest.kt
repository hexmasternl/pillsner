package nl.hexmaster.pillsner.domain.labelscan

import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.domain.model.DoseUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Spec: medicine-label-scan, "A recognized photo opens a new Add medicine form prefilled from it". */
class ParseLabelTextTest {

    @Test
    fun clearNameOnly_prefillsOnlyTheName() {
        val result = ParseLabelText.parse("Amoxicillin", AppLanguage.ENGLISH)

        assertEquals("Amoxicillin", result.name)
        assertNull(result.amountText)
        assertNull(result.unit)
    }

    @Test
    fun nameAndValidDose_prefillsAllThree() {
        val result = ParseLabelText.parse(
            rawText = "Amoxicillin\n500 mg\nTake with food",
            language = AppLanguage.ENGLISH,
        )

        assertEquals("Amoxicillin", result.name)
        assertEquals("500", result.amountText)
        assertEquals(DoseUnit.MILLIGRAM, result.unit)
    }

    @Test
    fun nameAndDoseOnTheSameLine_areBothRecognized() {
        val result = ParseLabelText.parse("Ibuprofen 200mg", AppLanguage.ENGLISH)

        assertEquals("Ibuprofen", result.name)
        assertEquals("200", result.amountText)
        assertEquals(DoseUnit.MILLIGRAM, result.unit)
    }

    @Test
    fun noUsableText_yieldsAnEmptyResult() {
        val result = ParseLabelText.parse("||||| #4592-XZ ***", AppLanguage.ENGLISH)

        assertNull(result.name)
        assertNull(result.amountText)
        assertNull(result.unit)
    }

    @Test
    fun ambiguousOrGarbledText_yieldsNoDose() {
        // OCR noise: a number followed by a word that is not a known unit anywhere.
        val result = ParseLabelText.parse("%&/ 250 xqzw #!?", AppLanguage.ENGLISH)

        assertNull(result.amountText)
        assertNull(result.unit)
    }

    @Test
    fun dutchUnitWordIsRecognized() {
        val result = ParseLabelText.parse(
            rawText = "Paracetamol\n2 tabletten",
            language = AppLanguage.DUTCH,
        )

        assertEquals("Paracetamol", result.name)
        assertEquals("2", result.amountText)
        assertEquals(DoseUnit.TABLET, result.unit)
    }

    @Test
    fun germanUnitWordIsRecognized() {
        val result = ParseLabelText.parse(
            rawText = "Ibuprofen\n400 mg Tabletten",
            language = AppLanguage.GERMAN,
        )

        assertEquals("Ibuprofen", result.name)
        assertEquals("400", result.amountText)
        assertEquals(DoseUnit.MILLIGRAM, result.unit)
    }

    @Test
    fun frenchUnitWordIsRecognized() {
        val result = ParseLabelText.parse(
            rawText = "Paracetamol\n1 comprime",
            language = AppLanguage.FRENCH,
        )

        assertEquals("Paracetamol", result.name)
        assertEquals("1", result.amountText)
        assertEquals(DoseUnit.TABLET, result.unit)
    }

    @Test
    fun frenchAccentedUnitWordIsRecognized() {
        val result = ParseLabelText.parse("2 comprimés", AppLanguage.FRENCH)

        assertEquals("2", result.amountText)
        assertEquals(DoseUnit.TABLET, result.unit)
    }

    @Test
    fun spanishUnitWordIsRecognized() {
        val result = ParseLabelText.parse(
            rawText = "Amoxicilina\n5 gotas",
            language = AppLanguage.SPANISH,
        )

        assertEquals("Amoxicilina", result.name)
        assertEquals("5", result.amountText)
        assertEquals(DoseUnit.DROP, result.unit)
    }

    @Test
    fun portugueseUnitWordIsRecognized() {
        val result = ParseLabelText.parse(
            rawText = "Amoxicilina\n1 comprimido",
            language = AppLanguage.PORTUGUESE,
        )

        assertEquals("Amoxicilina", result.name)
        assertEquals("1", result.amountText)
        assertEquals(DoseUnit.TABLET, result.unit)
    }

    @Test
    fun blankText_yieldsAnEmptyResult() {
        val result = ParseLabelText.parse("", AppLanguage.ENGLISH)

        assertNull(result.name)
        assertNull(result.amountText)
        assertNull(result.unit)
    }

    @Test
    fun systemLanguage_fallsBackToEnglishVocabulary() {
        val result = ParseLabelText.parse("Paracetamol\n2 tablets", AppLanguage.SYSTEM)

        assertEquals("2", result.amountText)
        assertEquals(DoseUnit.TABLET, result.unit)
    }
}
