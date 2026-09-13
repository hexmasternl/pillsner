package nl.hexmaster.pillsner.ui.medicines

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale

/**
 * Reads and writes the numeric part of a dose in the user's locale: "2.5" in English, "2,5" in
 * Dutch. Deliberately free of any Android type, so the view models that parse what the user types
 * stay unit-testable.
 */
class AmountParser(private val locale: Locale = Locale.getDefault()) {

    private val format: DecimalFormat = (NumberFormat.getNumberInstance(locale) as DecimalFormat).apply {
        isGroupingUsed = false
        maximumFractionDigits = MAX_FRACTION_DIGITS
        minimumFractionDigits = 0
        isParseBigDecimal = true
    }

    private val decimalSeparator: String =
        DecimalFormatSymbols.getInstance(locale).decimalSeparator.toString()

    /** The number as it should appear in an input field, without trailing zeros. */
    fun format(value: BigDecimal): String = format.format(value.stripTrailingZeros())

    /**
     * What the user typed, or null when it is not a number in this locale. A trailing decimal
     * separator ("2," while typing "2,5") reads as the part before it, so a half-typed amount is
     * never rejected mid-keystroke.
     */
    fun parse(text: String): BigDecimal? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val position = ParsePosition(0)
        val parsed = format.parse(trimmed, position) as? BigDecimal ?: return null
        val remainder = trimmed.substring(position.index)
        return if (remainder.isEmpty() || remainder == decimalSeparator) parsed else null
    }

    private companion object {
        const val MAX_FRACTION_DIGITS = 3
    }
}
