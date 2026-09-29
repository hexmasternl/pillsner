package nl.hexmaster.pillsner.domain.labelscan

import java.math.BigDecimal
import nl.hexmaster.pillsner.domain.model.Quantity

/**
 * A `<number><unit>` pair found on a line (design D4 rule 1).
 *
 * @property quantity the amount and unit it names.
 * @property isStrength true for a mass or volume unit (the label's strength, "50 mg"); false for a
 *   form unit (a count, "1 tablet").
 */
internal data class DoseToken(val quantity: Quantity, val isStrength: Boolean)

/** Finds dose tokens on a normalised line. */
internal object DoseTokens {

    /** Every `<number><unit>` on [normalisedLine] whose unit is in the vocabulary, in order. */
    fun extract(normalisedLine: String): List<DoseToken> = TOKEN.findAll(normalisedLine).mapNotNull { match ->
        val unit = LabelVocabulary.units[match.groupValues[2]] ?: return@mapNotNull null
        val isStrength = unit in LabelVocabulary.strengthUnits
        val number = match.groupValues[1]
        // "one tablet", "een tablet", "un comprimé": a spelled-out count. A strength is never spelled out.
        val value = parseNumber(number)
            ?: LabelVocabulary.numberWords[number]?.takeUnless { isStrength }?.toBigDecimal()
            ?: return@mapNotNull null
        if (value <= BigDecimal.ZERO) return@mapNotNull null
        DoseToken(Quantity(value, unit), isStrength)
    }.toList()

    /**
     * A label's number: an integer, a decimal with `.` or `,`, a simple fraction such as `1/2`, or
     * one of the fraction characters. Null when [text] is none of those, and null for a fraction
     * such as `1/3` that has no exact decimal: a dose is never rounded, so the field is left for
     * the user rather than filled with an approximation.
     */
    fun parseNumber(text: String): BigDecimal? = when (text) {
        "½" -> BigDecimal("0.5")
        "¼" -> BigDecimal("0.25")
        "¾" -> BigDecimal("0.75")
        else -> if ('/' in text) {
            val (numerator, denominator) = text.split('/')
            val divisor = denominator.toBigDecimalOrNull()?.takeIf { it.signum() != 0 } ?: return null
            try {
                numerator.toBigDecimalOrNull()?.divide(divisor)?.stripTrailingZeros()
            } catch (nonTerminating: ArithmeticException) {
                null
            }
        } else {
            text.replace(',', '.').toBigDecimalOrNull()
        }
    }

    /** A number as a label writes it, used by the frequency and date patterns too. */
    const val NUMBER = "(\\d+(?:[.,]\\d+)?|\\d+/\\d+|½|¼|¾)"

    /** A number or a number word, optional space, then a run of letters that has to be a whole word. */
    private val TOKEN = Regex(
        "(?<![\\p{L}\\p{N}])(\\d+(?:[.,]\\d+)?|\\d+/\\d+|½|¼|¾|" +
            LabelVocabulary.alternation(LabelVocabulary.numberWords.keys) +
            ")\\s*([\\p{L}]+)(?![\\p{L}\\p{N}])",
    )
}
