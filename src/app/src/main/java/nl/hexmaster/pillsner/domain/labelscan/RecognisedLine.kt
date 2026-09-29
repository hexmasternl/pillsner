package nl.hexmaster.pillsner.domain.labelscan

/**
 * One line of text as the recogniser read it, in reading order (medicine-label-photo-prefill
 * design D3, D4).
 *
 * The interpretation never logs this: it is whatever was printed on a medicine label, which is
 * the user's medical data.
 *
 * @property text the line as recognised, untrimmed.
 * @property confidence the recogniser's mean confidence for the line, 0 to 100.
 */
data class RecognisedLine(val text: String, val confidence: Float) {

    /** Debug only. Never log this. */
    override fun toString(): String = "RecognisedLine(${text.length} chars, $confidence)"
}
