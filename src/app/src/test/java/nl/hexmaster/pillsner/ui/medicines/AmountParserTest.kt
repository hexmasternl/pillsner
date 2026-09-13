package nl.hexmaster.pillsner.ui.medicines

import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Locale-aware reading and writing of a dose amount. */
class AmountParserTest {

    private val english = AmountParser(Locale.UK)
    private val dutch = AmountParser(Locale.forLanguageTag("nl-NL"))

    @Test
    fun `a whole number reads back as itself`() {
        assertEquals(BigDecimal("40"), english.parse("40"))
    }

    @Test
    fun `an English decimal uses a point`() {
        assertEquals(BigDecimal("2.5"), english.parse("2.5"))
        assertEquals("2.5", english.format(BigDecimal("2.5")))
    }

    @Test
    fun `a Dutch decimal uses a comma`() {
        assertEquals(BigDecimal("2.5"), dutch.parse("2,5"))
        assertEquals("2,5", dutch.format(BigDecimal("2.5")))
    }

    @Test
    fun `a half-typed decimal is not rejected`() {
        assertEquals(BigDecimal("2"), english.parse("2."))
        assertEquals(BigDecimal("2"), dutch.parse("2,"))
    }

    @Test
    fun `text that is not a number reads as nothing`() {
        assertNull(english.parse("abc"))
        assertNull(english.parse("40 mg"))
        assertNull(english.parse(""))
    }

    @Test
    fun `trailing zeros are not shown`() {
        assertEquals("1", english.format(BigDecimal("1.000")))
    }

    @Test
    fun `zero and negative amounts parse, so the validator can report them`() {
        assertEquals(0, english.parse("0")?.compareTo(BigDecimal.ZERO))
        assertEquals(-1, english.parse("-1")?.compareTo(BigDecimal.ZERO))
    }
}
