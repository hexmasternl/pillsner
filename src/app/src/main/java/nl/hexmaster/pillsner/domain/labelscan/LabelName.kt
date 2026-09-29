package nl.hexmaster.pillsner.domain.labelscan

/** Picks the medicine name out of the recognised lines (design D4 rule 5). */
internal object LabelName {

    /** The form's name field length; a longer read is cut, not rejected. */
    const val MAX_LENGTH = 60

    /**
     * The first candidate line that shares a line with a strength token, otherwise the first
     * candidate at all, or null when no line qualifies.
     *
     * @param lines the recognised lines as read, in reading order.
     */
    fun choose(lines: List<String>): String? {
        val candidates = lines.mapNotNull { line ->
            val normalised = LabelVocabulary.normalise(line)
            if (isNoise(normalised)) return@mapNotNull null
            val tokens = DoseTokens.extract(normalised)
            val cleaned = strip(line)
            if (cleaned.count { it.isLetter() } < MIN_LETTERS) return@mapNotNull null
            Candidate(cleaned, hasStrength = tokens.any { it.isStrength })
        }
        val chosen = candidates.firstOrNull { it.hasStrength } ?: candidates.firstOrNull()
        return chosen?.text?.take(MAX_LENGTH)?.trim()
    }

    /**
     * Whether a normalised line is something other than a name: a pharmacy or salutation word, a
     * phone number or postcode, only a date or only numbers, or an instruction line.
     */
    fun isNoise(normalised: String): Boolean {
        val words = LabelVocabulary.words(normalised)
        return words.any { it in LabelVocabulary.noiseWords } ||
            '@' in normalised ||
            PHONE.containsMatchIn(normalised) ||
            DUTCH_POSTCODE.containsMatchIn(normalised) ||
            POSTCODE_AND_TOWN.containsMatchIn(normalised) ||
            ONLY_NUMBERS.matches(normalised) ||
            LabelDates.isOnlyDate(normalised) ||
            LabelFrequency.detect(normalised) != null
    }

    /**
     * The line with every dose token, pack-size token, bare form word and form descriptor removed,
     * outer punctuation trimmed and whitespace collapsed. Case is kept as recognised.
     */
    private fun strip(line: String): String {
        val words = line.split(WHITESPACE).filter { it.isNotEmpty() }
        val kept = mutableListOf<String>()
        var index = 0
        while (index < words.size) {
            val word = words[index]
            val key = LabelVocabulary.normalise(word).trim(*PUNCTUATION)
            val next = words.getOrNull(index + 1)?.let { LabelVocabulary.normalise(it).trim(*PUNCTUATION) }
            when {
                // "50 mg", "30 st": a number followed by a unit or pack-size word drops both.
                isNumber(key) && next != null && (next in LabelVocabulary.units || next in LabelVocabulary.packSizeWords) -> index++
                // "50mg", "30st": the same, glued together.
                GLUED_TOKEN.matchEntire(key)?.let { it.groupValues[2] in LabelVocabulary.units || it.groupValues[2] in LabelVocabulary.packSizeWords } == true -> Unit
                key in LabelVocabulary.units && LabelVocabulary.units.getValue(key) !in LabelVocabulary.strengthUnits -> Unit
                key in LabelVocabulary.formDescriptors -> Unit
                else -> kept += word
            }
            index++
        }
        return kept.joinToString(" ").trim(*PUNCTUATION, ' ')
    }

    private fun isNumber(key: String): Boolean = NUMBER.matches(key)

    private data class Candidate(val text: String, val hasStrength: Boolean)

    private const val MIN_LETTERS = 3
    private val WHITESPACE = Regex("\\s+")
    private val NUMBER = Regex(DoseTokens.NUMBER)
    private val GLUED_TOKEN = Regex("${DoseTokens.NUMBER}(\\p{L}+)")
    private val PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', '-', '(', ')', '[', ']', '*', '"', '\'', '/')

    /** Seven or more digits with the usual separators: a phone number. */
    private val PHONE = Regex("(?<!\\d)\\+?\\d[\\d\\s().-]{6,}\\d(?!\\d)")

    /** "1234 AB": a Dutch postcode. */
    private val DUTCH_POSTCODE = Regex("\\b\\d{4}\\s?[a-z]{2}\\b")

    /** "10115 Berlin", "75001 Paris": a five-digit postcode followed by a town. */
    private val POSTCODE_AND_TOWN = Regex("\\b\\d{5}\\s+\\p{L}{3,}")

    private val ONLY_NUMBERS = Regex("[\\d\\s\\p{Punct}]*")
}
