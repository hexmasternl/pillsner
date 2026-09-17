package nl.hexmaster.pillsner.domain.labelscan

import java.text.Normalizer
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.SupportedLanguages

/**
 * The unit words [ParseLabelText] recognizes, one list per language Pillsner supports
 * (medicine-add-label-scan design D2). Keyed by the app's current language setting, not by
 * anything read from the label itself: the label may be in a different language than the app is
 * set to, and guessing at that would add complexity for a feature that always ends in user review.
 *
 * This is the one place the vocabulary is expected to drift out of date as wording is refined; see
 * task 6.2 of the medicine-add-label-scan change for the follow-up pass against realistic labels.
 */
internal object UnitVocabulary {

    /** [word] with case and accents removed, so vocabulary lookups do not depend on either. */
    fun normalize(word: String): String {
        val decomposed = Normalizer.normalize(word.lowercase(), Normalizer.Form.NFD)
        return DIACRITIC_PATTERN.replace(decomposed, "")
    }

    /** The unit-word lookup for [language], falling back to [SupportedLanguages.fallback]. */
    fun forLanguage(language: AppLanguage): Map<String, DoseUnit> =
        byLanguage[language] ?: byLanguage.getValue(SupportedLanguages.fallback)

    private val DIACRITIC_PATTERN = Regex("\\p{Mn}+")

    private fun vocabulary(vararg entries: Pair<DoseUnit, List<String>>): Map<String, DoseUnit> =
        entries.flatMap { (unit, words) -> words.map { normalize(it) to unit } }.toMap()

    private val byLanguage: Map<AppLanguage, Map<String, DoseUnit>> = mapOf(
        AppLanguage.ENGLISH to vocabulary(
            DoseUnit.MILLIGRAM to listOf("mg", "milligram", "milligrams"),
            DoseUnit.GRAM to listOf("g", "gram", "grams"),
            DoseUnit.MICROGRAM to listOf("mcg", "microgram", "micrograms", "ug", "µg"),
            DoseUnit.MILLILITRE to listOf("ml", "milliliter", "milliliters", "millilitre", "millilitres"),
            DoseUnit.TABLET to listOf("tablet", "tablets", "tab", "tabs"),
            DoseUnit.CAPSULE to listOf("capsule", "capsules", "cap", "caps"),
            DoseUnit.DROP to listOf("drop", "drops"),
            DoseUnit.PUFF to listOf("puff", "puffs"),
            DoseUnit.UNIT to listOf("unit", "units", "iu"),
        ),
        AppLanguage.DUTCH to vocabulary(
            DoseUnit.MILLIGRAM to listOf("mg", "milligram"),
            DoseUnit.GRAM to listOf("g", "gram"),
            DoseUnit.MICROGRAM to listOf("mcg", "microgram", "ug", "µg"),
            DoseUnit.MILLILITRE to listOf("ml", "milliliter"),
            DoseUnit.TABLET to listOf("tablet", "tabletten"),
            DoseUnit.CAPSULE to listOf("capsule", "capsules"),
            DoseUnit.DROP to listOf("druppel", "druppels"),
            DoseUnit.PUFF to listOf("pufje", "pufjes", "inhalatie", "inhalaties"),
            DoseUnit.UNIT to listOf("eenheid", "eenheden"),
        ),
        AppLanguage.GERMAN to vocabulary(
            DoseUnit.MILLIGRAM to listOf("mg", "milligramm"),
            DoseUnit.GRAM to listOf("g", "gramm"),
            DoseUnit.MICROGRAM to listOf("mcg", "mikrogramm", "ug", "µg"),
            DoseUnit.MILLILITRE to listOf("ml", "milliliter"),
            DoseUnit.TABLET to listOf("tablette", "tabletten"),
            DoseUnit.CAPSULE to listOf("kapsel", "kapseln"),
            DoseUnit.DROP to listOf("tropfen"),
            DoseUnit.PUFF to listOf("hub", "hube", "hübe"),
            DoseUnit.UNIT to listOf("einheit", "einheiten"),
        ),
        AppLanguage.FRENCH to vocabulary(
            DoseUnit.MILLIGRAM to listOf("mg", "milligramme", "milligrammes"),
            DoseUnit.GRAM to listOf("g", "gramme", "grammes"),
            DoseUnit.MICROGRAM to listOf("mcg", "microgramme", "microgrammes", "ug", "µg"),
            DoseUnit.MILLILITRE to listOf("ml", "millilitre", "millilitres"),
            DoseUnit.TABLET to listOf("comprime", "comprimes"),
            DoseUnit.CAPSULE to listOf("gelule", "gelules"),
            DoseUnit.DROP to listOf("goutte", "gouttes"),
            DoseUnit.PUFF to listOf("bouffee", "bouffees"),
            DoseUnit.UNIT to listOf("unite", "unites"),
        ),
        AppLanguage.SPANISH to vocabulary(
            DoseUnit.MILLIGRAM to listOf("mg", "miligramo", "miligramos"),
            DoseUnit.GRAM to listOf("g", "gramo", "gramos"),
            DoseUnit.MICROGRAM to listOf("mcg", "microgramo", "microgramos", "ug", "µg"),
            DoseUnit.MILLILITRE to listOf("ml", "mililitro", "mililitros"),
            DoseUnit.TABLET to listOf("comprimido", "comprimidos"),
            DoseUnit.CAPSULE to listOf("capsula", "capsulas"),
            DoseUnit.DROP to listOf("gota", "gotas"),
            DoseUnit.PUFF to listOf("inhalacion", "inhalaciones", "pulsacion", "pulsaciones"),
            DoseUnit.UNIT to listOf("unidad", "unidades"),
        ),
        AppLanguage.PORTUGUESE to vocabulary(
            DoseUnit.MILLIGRAM to listOf("mg", "miligrama", "miligramas"),
            DoseUnit.GRAM to listOf("g", "grama", "gramas"),
            DoseUnit.MICROGRAM to listOf("mcg", "micrograma", "microgramas", "ug", "µg"),
            DoseUnit.MILLILITRE to listOf("ml", "mililitro", "mililitros"),
            DoseUnit.TABLET to listOf("comprimido", "comprimidos"),
            DoseUnit.CAPSULE to listOf("capsula", "capsulas"),
            DoseUnit.DROP to listOf("gota", "gotas"),
            DoseUnit.PUFF to listOf("jato", "jatos"),
            DoseUnit.UNIT to listOf("unidade", "unidades"),
        ),
    )
}
