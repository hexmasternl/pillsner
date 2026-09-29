package nl.hexmaster.pillsner.domain.labelscan

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Design D4 rule 5: which line is the medicine name. */
class LabelNameTest {

    private val today = LocalDate.of(2026, 9, 29)
    private val interpret = InterpretLabelText()

    @Test
    fun `a Dutch pharmacy label yields the name from the strength line`() {
        val result = interpret(
            lines(
                "Apotheek De Linde",
                "Dhr. J. Jansen",
                "1234 AB Utrecht",
                "Tel. 030-1234567",
                "27-09-2026",
                "METOPROLOL 50 MG TABLET",
                "2x daags 1 tablet",
                "30 tabletten",
            ),
            today,
        )

        assertEquals("METOPROLOL", result.name)
    }

    @Test
    fun `a box front without a strength on the name line yields the first candidate`() {
        val result = interpret(lines("Paracetamol", "500 mg", "20 tabletten"), today)

        assertEquals("Paracetamol", result.name)
    }

    @Test
    fun `noise lines before the name are skipped`() {
        val result = interpret(
            lines("Farmacia Central", "Sra. Ana García", "Tel 91 123 45 67", "28/09/2026", "IBUPROFENO 400 mg comprimidos", "1 comprimido cada 8 horas"),
            today,
        )

        assertEquals("IBUPROFENO", result.name)
    }

    @Test
    fun `the line with the strength is preferred over an earlier candidate`() {
        assertEquals("Loratadine", interpret(lines("Sinuslim forte", "Loratadine 10 mg"), today).name)
    }

    @Test
    fun `dose tokens, pack sizes and form words are removed and case is kept`() {
        assertEquals("Amoxicilline", LabelName.choose(listOf("Amoxicilline 500 mg 30 st")))
        assertEquals("SIMVASTATINE", LabelName.choose(listOf("SIMVASTATINE 20 MG FILMOMHULDE TABLETTEN")))
        assertEquals("Calmirex", LabelName.choose(listOf("Calmirex 20 mg comprimés pelliculés")))
        assertEquals("Norvelin", LabelName.choose(listOf("Norvelin 100mg Filmtabletten")))
        assertEquals("Vitamine B12", LabelName.choose(listOf("Vitamine B12 1000 µg")))
    }

    @Test
    fun `trailing punctuation is trimmed and the name is capped at sixty characters`() {
        assertEquals("Zorvalex", LabelName.choose(listOf("Zorvalex 50 mg,")))
        val long = "A".repeat(70)
        assertEquals(60, LabelName.choose(listOf(long))?.length)
    }

    @Test
    fun `a candidate needs at least three letters`() {
        assertNull(LabelName.choose(listOf("A1", "12345", "27-09-2026", "50 mg")))
    }

    @Test
    fun `noise is a pharmacy, a salutation, a phone number, a postcode, a date or an instruction`() {
        assertTrue(noise("Apotheek De Linde"))
        assertTrue(noise("Pharmacie du Centre"))
        assertTrue(noise("Mevr. B. Voorbeeld"))
        assertTrue(noise("Herr C. Beispiel"))
        assertTrue(noise("Tel. 030-1234567"))
        assertTrue(noise("+31 6 12345678"))
        assertTrue(noise("1234 AB Utrecht"))
        assertTrue(noise("10115 Berlin"))
        assertTrue(noise("27-09-2026"))
        assertTrue(noise("Dispensed 27/09/2026"))
        assertTrue(noise("2x daags 1 tablet"))
        assertTrue(noise("info@apotheek.nl"))
        assertTrue(noise("Lot A1B2C3"))
        assertTrue(noise("Take with food"))
        assertTrue(noise("30 tabletten").not())
        assertTrue(noise("METOPROLOL 50 MG TABLET").not())
    }

    @Test
    fun `nothing usable leaves the name absent`() {
        val result = interpret(lines("Apotheek De Linde", "Tel. 030-1234567", "27-09-2026"), today)

        assertNull(result.name)
        assertTrue(result.isEmpty)
    }

    private fun noise(text: String) = LabelName.isNoise(LabelVocabulary.normalise(text))

    private fun lines(vararg texts: String) = texts.map { RecognisedLine(it, 90f) }
}
