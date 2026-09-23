package nl.hexmaster.pillsner.ui.medicines

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.math.BigDecimal
import java.util.Locale
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity

/**
 * Writes a dose amount the way the user reads it: "40 mg", "2.5 ml", "1 tablet".
 *
 * The unit label comes from a plural resource so "1 tablet" and "2 tablets" are both right and a
 * translator can give a language with more plural forms what it needs. Units that are never
 * pluralised in English, such as mg, simply repeat the same text. The number itself is the
 * [AmountParser]'s job, which is also what the input fields use, so what is shown and what is
 * typed always agree.
 */
class QuantityFormatter(
    private val context: Context,
    locale: Locale = Locale.getDefault(),
    private val amountParser: AmountParser = AmountParser(locale),
) {

    fun format(quantity: Quantity): String = format(quantity.value, quantity.unit)

    /**
     * The same formatting as [format], for a raw amount and unit rather than a [Quantity] — needed
     * for a stock batch's remaining amount, which may legitimately be zero and so cannot always be
     * wrapped in a [Quantity] (`medicine-stock-tracking`).
     */
    fun format(value: BigDecimal, unit: DoseUnit): String = context.resources.getQuantityString(
        unit.pluralRes(),
        value.pluralCount(),
        amountParser.format(value),
    )

    /** Just the number, as it should appear in an input field. */
    fun formatAmount(value: BigDecimal): String = amountParser.format(value)

    private companion object {
        /**
         * Android plural selection takes a whole number, so an amount of exactly one picks the
         * singular and everything else, 0.5 included, picks the general form.
         */
        fun BigDecimal.pluralCount(): Int = if (compareTo(BigDecimal.ONE) == 0) 1 else 2

        @PluralsRes
        fun DoseUnit.pluralRes(): Int = when (this) {
            DoseUnit.MILLIGRAM -> R.plurals.dose_unit_milligram
            DoseUnit.GRAM -> R.plurals.dose_unit_gram
            DoseUnit.MICROGRAM -> R.plurals.dose_unit_microgram
            DoseUnit.MILLILITRE -> R.plurals.dose_unit_millilitre
            DoseUnit.TABLET -> R.plurals.dose_unit_tablet
            DoseUnit.CAPSULE -> R.plurals.dose_unit_capsule
            DoseUnit.DROP -> R.plurals.dose_unit_drop
            DoseUnit.PUFF -> R.plurals.dose_unit_puff
            DoseUnit.UNIT -> R.plurals.dose_unit_unit
        }
    }
}

/** The short label of a unit for the unit picker: "mg", "tablet". */
@StringRes
fun DoseUnit.labelRes(): Int = when (this) {
    DoseUnit.MILLIGRAM -> R.string.dose_unit_label_milligram
    DoseUnit.GRAM -> R.string.dose_unit_label_gram
    DoseUnit.MICROGRAM -> R.string.dose_unit_label_microgram
    DoseUnit.MILLILITRE -> R.string.dose_unit_label_millilitre
    DoseUnit.TABLET -> R.string.dose_unit_label_tablet
    DoseUnit.CAPSULE -> R.string.dose_unit_label_capsule
    DoseUnit.DROP -> R.string.dose_unit_label_drop
    DoseUnit.PUFF -> R.string.dose_unit_label_puff
    DoseUnit.UNIT -> R.string.dose_unit_label_unit
}
