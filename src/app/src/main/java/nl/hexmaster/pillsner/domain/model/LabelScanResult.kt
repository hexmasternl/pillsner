package nl.hexmaster.pillsner.domain.model

/**
 * A best-effort guess pulled from a photographed medicine label's recognized text
 * (medicine-add-label-scan design D3).
 *
 * Every field is provisional and optional: a field is only ever set when it could be read with
 * some confidence, and even then it is nothing more than what a keystroke would have produced. A
 * caller MUST treat every field as an ordinary, editable draft value, subject to the same review
 * and the same validation a typed value gets, never as something to save directly.
 *
 * @property name the recognized medicine name, or null when no usable name line was found.
 * @property amountText the recognized dose amount, in the same free-text form the add-medicine
 *   form's dose field accepts, or null when no amount could be confidently paired with a known
 *   unit word. Left as text, not parsed to a number, because the number format on a label may not
 *   match the app's own locale; the form's existing amount parsing and validation decide whether
 *   it is usable, exactly as they would for something the user typed.
 * @property unit the recognized dose unit, or null when [amountText] is also null.
 */
data class LabelScanResult(
    val name: String? = null,
    val amountText: String? = null,
    val unit: DoseUnit? = null,
)
