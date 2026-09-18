package nl.hexmaster.pillsner.domain.labelscan

import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.LabelScanResult

/**
 * Turns raw text recognized from a photographed medicine label into a best-effort
 * [LabelScanResult] (medicine-add-label-scan design D3).
 *
 * This is a typing shortcut, not clinical guidance: it never validates a dose, never checks it
 * against any medical reference, and never tries to identify a drug (medicine-label-scan spec, "No
 * medical interpretation of recognized text"). A field is only ever set when it can be read with
 * some confidence; otherwise it stays null and the caller falls back to its own normal empty
 * default, exactly as if nothing had been typed.
 */
object ParseLabelText {

    /** A number, optionally followed by whitespace and a word — a candidate "amount unit" pair. */
    private val AMOUNT_AND_WORD = Regex("""(\d+(?:[.,]\d+)?)\s*([\p{L}]+)""")

    private const val MIN_NAME_LENGTH = 2
    private const val MIN_NAME_LETTER_RATIO = 0.6

    fun parse(rawText: String, language: AppLanguage): LabelScanResult {
        val vocabulary = UnitVocabulary.forLanguage(language)
        val lines = rawText.lines().map(String::trim).filter(String::isNotEmpty)

        val dose = lines.firstNotNullOfOrNull { line -> findDose(line, vocabulary) }
        val name = lines
            .map(::stripAmountAndWord)
            .firstOrNull(::looksLikeName)
            ?.trim()

        return LabelScanResult(name = name, amountText = dose?.first, unit = dose?.second)
    }

    private fun findDose(line: String, vocabulary: Map<String, DoseUnit>) =
        AMOUNT_AND_WORD.findAll(line).firstNotNullOfOrNull { match ->
            vocabulary[UnitVocabulary.normalize(match.groupValues[2])]
                ?.let { unit -> match.groupValues[1] to unit }
        }

    private fun stripAmountAndWord(line: String): String = AMOUNT_AND_WORD.replace(line, "")

    /**
     * A line only counts as a plausible name once the amount/unit has been stripped from it: it
     * must have some length left and be mostly letters, so a barcode's digits or a line of pure
     * symbols never becomes a "name" (medicine-label-scan spec, "Nothing usable is recognized").
     */
    private fun looksLikeName(candidate: String): Boolean {
        if (candidate.trim().length < MIN_NAME_LENGTH) return false
        val nonBlank = candidate.count { !it.isWhitespace() }
        if (nonBlank == 0) return false
        val letters = candidate.count(Char::isLetter)
        return letters.toDouble() / nonBlank >= MIN_NAME_LETTER_RATIO
    }
}
