package nl.hexmaster.pillsner.domain.labelscan

import java.text.Normalizer
import java.time.LocalTime
import java.util.Locale
import nl.hexmaster.pillsner.domain.model.DoseUnit

/**
 * The words the interpretation recognises, in English, Dutch, German, French, Spanish and
 * Portuguese at once (medicine-label-photo-prefill design D4).
 *
 * One table rather than one per app language, because the label was printed in the pharmacy's
 * language, not the phone's. Every word is stored in its [normalise]d form and every lookup goes
 * through [normalise] too, so "Comprimé", "comprime" and "COMPRIMÉ" all match: a recogniser that
 * drops an accent does not lose the match.
 */
internal object LabelVocabulary {

    /** Lower case with every accent stripped, so matching is case- and accent-insensitive. */
    fun normalise(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(COMBINING_MARKS, "").lowercase(Locale.ROOT)

    /** The words of a normalised line: runs of letters and digits, punctuation dropped. */
    fun words(normalised: String): List<String> = WORD.findAll(normalised).map { it.value }.toList()

    /** A regex alternation of [words], longest first so "tot en met" wins over "tot". */
    fun alternation(words: Collection<String>): String =
        words.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }

    /** Unit words to the dose unit they mean. */
    val units: Map<String, DoseUnit> = buildMap {
        put(DoseUnit.MILLIGRAM, "mg", "milligram", "milligrams", "milligramme", "milligrammes", "milligramm", "miligramo", "miligramos", "miligrama", "miligramas")
        put(DoseUnit.GRAM, "g", "gr", "gram", "grams", "gramm", "gramme", "grammes", "gramo", "gramos", "grama", "gramas")
        put(DoseUnit.MICROGRAM, "mcg", "µg", "μg", "ug", "microgram", "micrograms", "microgramme", "microgrammes", "mikrogramm", "microgramo", "microgramos", "micrograma", "microgramas")
        put(DoseUnit.MILLILITRE, "ml", "millilitre", "millilitres", "milliliter", "milliliters", "mililitro", "mililitros")
        put(DoseUnit.TABLET, "tablet", "tablets", "tabletten", "tablette", "tabl", "tab", "tabs", "tbl", "comprime", "comprimes", "comprimido", "comprimidos", "compr", "cp", "cpr", "pastilla", "pastillas", "dragee", "dragees")
        put(DoseUnit.CAPSULE, "capsule", "capsules", "caps", "cap", "kapsel", "kapseln", "gelule", "gelules", "capsula", "capsulas")
        put(DoseUnit.DROP, "drop", "drops", "druppel", "druppels", "tropfen", "goutte", "gouttes", "gota", "gotas", "gtt")
        put(DoseUnit.PUFF, "puff", "puffs", "pufje", "pufjes", "inhalatie", "inhalaties", "inhalation", "inhalations", "hub", "hube", "spruhstoss", "spruhstoß", "spruhstosse", "spruhstoße", "bouffee", "bouffees", "inhalacion", "inhalaciones", "inalacao", "inalacoes")
        put(DoseUnit.UNIT, "unit", "units", "eenheid", "eenheden", "einheit", "einheiten", "unite", "unites", "unidad", "unidades", "unidade", "ie", "iu", "ui")
    }

    /** The units that state how much medicine is in one dose form: the label's strength. */
    val strengthUnits: Set<DoseUnit> =
        setOf(DoseUnit.MILLIGRAM, DoseUnit.GRAM, DoseUnit.MICROGRAM, DoseUnit.MILLILITRE)

    /** Words for a number of pieces in a pack ("30 st"), which is never a dose. */
    val packSizeWords: Set<String> = setOf("st", "stuks", "stk", "stck", "pcs", "pc", "pieces", "piece", "pz", "uds")

    /** Words that describe a dose form without a number in front ("filmomhulde tabletten"). */
    val formDescriptors: Set<String> = setOf(
        "filmomhulde", "omhulde", "filmtablet", "filmtabletten", "filmtablette", "retardtabletten", "retardtablette",
        "pellicule", "pellicules", "recubierto", "recubiertos", "recubierta", "recubiertas", "revestido", "revestidos",
        "uberzogen", "uberzogene",
        // Liquid forms, which follow a concentration on the name line ("250 mg/5 ml suspensie").
        "suspensie", "suspension", "suspensao", "drank", "siroop", "syrup", "sirop", "jarabe", "xarope", "saft",
        "oplossing", "losung", "solution", "solucion", "solucao", "druppelvloeistof",
    )

    /** The parts of the day a label names, and the clock time each one becomes (design D4 rule 4). */
    enum class DayPart(val slot: LocalTime) {
        MORNING(LocalTime.of(8, 0)),
        NOON(LocalTime.of(13, 0)),
        EVENING(LocalTime.of(18, 0)),
        NIGHT(LocalTime.of(22, 0)),
    }

    val dayParts: Map<String, DayPart> = buildMap {
        put(DayPart.MORNING, "morgens", "ochtend", "ochtends", "morning", "mornings", "morgen", "fruh", "matin", "manana", "mananas", "manha", "manhas")
        put(DayPart.NOON, "middags", "middag", "noon", "midday", "lunchtime", "lunch", "mittag", "mittags", "midi", "mediodia", "almoco", "almuerzo")
        put(DayPart.EVENING, "avonds", "avond", "evening", "evenings", "abend", "abends", "soir", "tarde", "tardes", "jantar", "cena")
        put(DayPart.NIGHT, "nachts", "nacht", "night", "bedtime", "nuit", "coucher", "noche", "noches", "noite", "noites", "slapen", "schlafen")
    }

    /** Number words a label spells out instead of writing a digit. */
    val numberWords: Map<String, Int> = buildMap {
        put(1, "een", "one", "ein", "eine", "einen", "einer", "une", "un", "una", "uma", "um")
        put(2, "twee", "two", "zwei", "deux", "dos", "duas", "dois")
        put(3, "drie", "three", "drei", "trois", "tres")
        put(4, "vier", "four", "quatre", "cuatro", "quatro")
        put(5, "vijf", "five", "funf", "cinq", "cinco")
        put(6, "zes", "six", "sechs", "seis")
        put(8, "acht", "eight", "huit", "ocho", "oito")
        put(12, "twaalf", "twelve", "zwolf", "douze", "doce")
    }

    /** "half a tablet", "een halve tablet", "un demi comprimé": a count of one half, spelled out. */
    val halfWords: Set<String> = setOf("half", "halve", "halbe", "halben", "demi", "demie", "media", "medio", "meia", "meio")

    /** Articles that may sit between a spelled-out count and its unit: "half a tablet". */
    val articles: Set<String> = setOf("a", "an", "een", "eine", "einen", "un", "une", "una", "um", "uma")

    /**
     * "As needed" in the six languages. A medicine taken this way has no schedule in Pillsner, so a
     * line carrying one of these yields no frequency, whatever else it says.
     */
    val asNeededPhrases: List<String> = listOf(
        "zo nodig", "indien nodig", "zonodig", "bij klachten", "as needed", "when needed", "if needed", "as required",
        "when required", "when necessary", "if necessary", "prn", "bei bedarf", "nach bedarf", "si besoin", "au besoin",
        "si necessaire", "en cas de besoin", "segun necesidad", "si es necesario", "a demanda", "cuando sea necesario",
        "se necessario", "quando necessario", "em caso de necessidade", "sos",
    )

    /**
     * Words that mark a line as an expiry or lot line, whose date is neither a start nor an end of
     * use: "EXP 03/2028", "houdbaar tot", "à utiliser avant".
     */
    val expiryWords: Set<String> = setOf(
        "exp", "expiry", "expires", "expiration", "houdbaar", "vervaldatum", "vervalt", "verfall", "verfallsdatum",
        "verwendbar", "haltbar", "peremption", "perime", "caduca", "caducidad", "validade", "valido", "valida", "lot",
        "batch", "charge", "chb",
    )

    /**
     * Verbs that make a line an instruction ("innemen", "take", "prendre"). A day-part word counts
     * as a rhythm only beside one of these, a count or a per-day word: "Night Nurse 500 mg" is a
     * product name, not a bedtime dose.
     */
    val instructionVerbs: Set<String> = setOf(
        "gebruik", "gebruiken", "innemen", "inname", "nemen", "neem", "slikken", "take", "taking", "use", "einnehmen",
        "nehmen", "anwenden", "prendre", "prenez", "prends", "tomar", "tome", "tomese", "toma", "ingerir", "avaler",
        "schlucken", "inhaleren", "inhale", "inhalieren", "inhaler", "inhalar", "druppelen",
    )

    /** Words that are a frequency in themselves: "twice", "driemaal", "einmal". */
    val timesWords: Map<String, Int> = buildMap {
        put(1, "once", "eenmaal", "einmal")
        put(2, "twice", "tweemaal", "zweimal")
        put(3, "thrice", "driemaal", "dreimal")
        put(4, "viermaal", "viermal")
    }

    /**
     * Words that mark a line as something other than the medicine name: the pharmacy, the patient,
     * contact details, lot and expiry lines, and instructions that carry no frequency.
     */
    val noiseWords: Set<String> = setOf(
        "apotheek", "apotheke", "pharmacy", "pharmacie", "farmacia", "drogerie", "drogist",
        // "m" is the French "M." once normalisation has dropped the full stop (design D4).
        "dhr", "mevr", "mw", "mr", "mrs", "ms", "herr", "frau", "m", "mme", "mlle", "sr", "sra", "srta", "dr",
        "tel", "fax", "www", "http", "https", "email", "bsn", "rx", "lot", "batch", "charge", "exp", "expiry",
        "vervaldatum", "houdbaar", "verfall", "verfallsdatum", "caduca", "caducidad", "validade", "peremption", "ch",
        "dispensed", "date", "datum", "fecha", "data", "afgeleverd", "afleverdatum", "abgabe", "abgabedatum",
        "delivre", "dispensado", "entregado",
        "gebruik", "gebruiken", "innemen", "inname", "nemen", "take", "taken", "use", "einnehmen", "nehmen",
        "prendre", "prenez", "tomar", "tome", "tomese", "ingerir", "avaler", "schlucken",
    )

    private fun <T> MutableMap<String, T>.put(value: T, vararg words: String) = words.forEach { put(it, value) }

    private val COMBINING_MARKS = Regex("\\p{Mn}+")
    private val WORD = Regex("[\\p{L}\\p{N}]+")
}
