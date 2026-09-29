package nl.hexmaster.pillsner.domain.labelscan

import java.math.BigDecimal
import java.time.LocalTime
import nl.hexmaster.pillsner.domain.labelscan.LabelVocabulary.DayPart
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule

/** How often a label says to take the medicine, before it becomes a [Schedule] (design D4 rule 4). */
internal sealed interface Frequency {
    /** "2x daags", "twice daily", "3 mal täglich". */
    data class TimesPerDay(val count: Int) : Frequency

    /** "om de 8 uur", "every 8 hours", "cada 8 horas". */
    data class EveryNHours(val hours: Int) : Frequency

    /** "om de dag", "every other day", "jeden zweiten Tag". */
    data object EveryOtherDay : Frequency

    /** "'s morgens en 's avonds", "matin et soir". */
    data class DayParts(val parts: Set<DayPart>) : Frequency

    /** "1-0-1", "1-1-1", "1-0-0-1": morning, noon, evening and, when present, night. */
    data class BoxNotation(val digits: List<Int>) : Frequency
}

/** Reads a frequency off a normalised line. A line that yields one is an *instruction line*. */
internal object LabelFrequency {

    /** The frequency on [line], or null when it has none. Box notation and intervals win over words. */
    fun detect(line: String): Frequency? {
        BOX.find(line)?.let { match ->
            return Frequency.BoxNotation(match.groupValues.drop(1).filter { it.isNotEmpty() }.map { it.toInt() })
        }
        (EVERY_N_HOURS.find(line) ?: EVERY_N_HOURS_PORTUGUESE.find(line))?.let { match ->
            return number(match.groupValues[1])?.let(Frequency::EveryNHours)
        }
        if (EVERY_OTHER_DAY.containsMatchIn(line)) return Frequency.EveryOtherDay
        val parts = LabelVocabulary.words(line).mapNotNull { LabelVocabulary.dayParts[it] }.toSet()
        if (parts.isNotEmpty()) return Frequency.DayParts(parts)
        (TIMES_PER_DAY_X.find(line) ?: TIMES_PER_DAY_WORD.find(line) ?: TIMES_PER_DAY_DD.find(line))?.let { match ->
            return number(match.groupValues[1])?.let(Frequency::TimesPerDay)
        }
        LabelVocabulary.words(line).firstNotNullOfOrNull { LabelVocabulary.timesWords[it] }?.let {
            return Frequency.TimesPerDay(it)
        }
        if (PER_DAY_ALONE.containsMatchIn(line)) return Frequency.TimesPerDay(1)
        return null
    }

    /** The first form unit named on [line] without a number in front, such as "Tablette" in "1-0-1 Tablette". */
    fun bareFormUnit(line: String): DoseUnit? = LabelVocabulary.words(line)
        .firstNotNullOfOrNull { word -> LabelVocabulary.units[word]?.takeIf { it !in LabelVocabulary.strengthUnits } }

    /** A digit string or a number word as an integer, or null. */
    fun number(text: String): Int? = text.toIntOrNull() ?: LabelVocabulary.numberWords[text]

    /** A digit string or a spelled-out number, as one capturing group. */
    private val NUMBER_PATTERN = "(\\d+|" + LabelVocabulary.alternation(LabelVocabulary.numberWords.keys) + ")"

    private val EVERY = LabelVocabulary.alternation(
        listOf(
            "om de", "elke", "iedere", "every", "each", "alle", "jede", "jeden", "toutes les", "tous les", "chaque",
            "cada", "a cada", "todas las", "todos os",
        ),
    )
    private val HOURS = LabelVocabulary.alternation(
        listOf("uur", "uren", "u", "hours", "hour", "hrs", "hr", "h", "stunden", "stunde", "std", "heures", "heure", "horas", "hora", "hs"),
    )
    private val TIMES = LabelVocabulary.alternation(listOf("maal", "keer", "times", "time", "mal", "fois", "veces", "vez", "vezes"))
    private val PER_DAY = LabelVocabulary.alternation(
        listOf(
            "per dag", "daags", "a day", "per day", "daily", "each day", "every day", "dagelijks", "elke dag",
            "iedere dag", "taglich", "pro tag", "am tag", "jeden tag", "par jour", "chaque jour", "tous les jours",
            "al dia", "por dia", "cada dia", "diario", "diaria", "ao dia", "todos os dias", "diariamente",
        ),
    )

    /** `1-0-1`, with or without spaces around the dashes, and never part of a date or a range of numbers. */
    private val BOX = Regex("(?<![\\p{N}.,/-])([0-4])\\s*-\\s*([0-4])\\s*-\\s*([0-4])(?:\\s*-\\s*([0-4]))?(?![\\p{N}.,/-])")
    private val EVERY_N_HOURS = Regex("\\b(?:$EVERY)\\s+$NUMBER_PATTERN\\s*(?:$HOURS)\\b")
    private val EVERY_N_HOURS_PORTUGUESE = Regex("\\bde\\s+$NUMBER_PATTERN\\s+em\\s+\\d+\\s*(?:$HOURS)\\b")
    private val EVERY_OTHER_DAY = Regex(
        "\\b(?:" + LabelVocabulary.alternation(
            listOf(
                "om de dag", "om de andere dag", "elke andere dag", "iedere andere dag", "om de twee dagen", "om de 2 dagen",
                "elke twee dagen", "elke 2 dagen", "every other day", "every second day", "every two days", "every 2 days",
                "on alternate days", "alternate days", "jeden zweiten tag", "alle zwei tage", "alle 2 tage",
                "tous les deux jours", "tous les 2 jours", "un jour sur deux", "cada dos dias", "cada 2 dias",
                "dia sim dia nao", "a cada dois dias", "a cada 2 dias", "de dois em dois dias", "de 2 em 2 dias",
            ),
        ) + ")\\b",
    )
    private val TIMES_PER_DAY_X = Regex("\\b$NUMBER_PATTERN\\s*(?:x|×)\\s*(?:$PER_DAY)\\b")
    private val TIMES_PER_DAY_WORD = Regex("\\b$NUMBER_PATTERN\\s*(?:$TIMES)\\b(?:\\s*(?:$PER_DAY)\\b)?")
    private val TIMES_PER_DAY_DD = Regex("\\b(\\d+)\\s*dd\\b")
    private val PER_DAY_ALONE = Regex("\\b(?:$PER_DAY)\\b")
}

/** Turns a [Frequency] and an amount into the app's schedule shapes (design D4 rule 4). */
internal object LabelSchedules {

    /** The every-N-hours intervals the schedule editor offers; the same list as `ScheduleDraft.HOUR_INTERVALS`. */
    val SUPPORTED_HOUR_INTERVALS: List<Int> = listOf(1, 2, 3, 4, 6, 8, 12, 24)

    /** Where an every-N-hours schedule starts, and the single daily time: the editor's own default. */
    val FIRST_DOSE: LocalTime = LocalTime.of(8, 0)

    private val DEFAULT_TIMES: Map<Int, List<LocalTime>> = mapOf(
        1 to listOf(LocalTime.of(8, 0)),
        2 to listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
        3 to listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)),
        4 to listOf(LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(16, 0), LocalTime.of(20, 0)),
    )

    /** The box notation's slots, in order: morning, noon, evening, night. */
    private val BOX_SLOTS = listOf(DayPart.MORNING, DayPart.NOON, DayPart.EVENING, DayPart.NIGHT)

    /**
     * The schedules for [frequency], or none when the frequency is outside the supported shapes or
     * no amount could be settled. Whatever the domain constructors reject is dropped, not patched.
     *
     * @param amount the count token on the instruction line, otherwise the default dose.
     * @param countUnit the form unit the label counts in, for box notation; null when it names none.
     * @param defaultDose the label's strength, which box notation multiplies when there is no count unit.
     */
    fun build(frequency: Frequency, amount: Quantity?, countUnit: DoseUnit?, defaultDose: Quantity?): List<Schedule> {
        val proposed: List<() -> Schedule> = when (frequency) {
            is Frequency.TimesPerDay -> timesPerDay(frequency.count, amount ?: return emptyList())
            is Frequency.EveryNHours -> {
                if (frequency.hours !in SUPPORTED_HOUR_INTERVALS) return emptyList()
                val quantity = amount ?: return emptyList()
                listOf { Schedule.EveryNHours(quantity, frequency.hours, FIRST_DOSE) }
            }
            Frequency.EveryOtherDay -> {
                val quantity = amount ?: return emptyList()
                listOf { Schedule.EveryNDays(quantity, 2, listOf(FIRST_DOSE)) }
            }
            is Frequency.DayParts -> {
                val quantity = amount ?: return emptyList()
                listOf { Schedule.EveryNDays(quantity, 1, frequency.parts.map { it.slot }.sorted()) }
            }
            is Frequency.BoxNotation -> box(frequency.digits, countUnit, defaultDose)
        }
        return proposed.mapNotNull { make -> runCatching(make).getOrNull() }
    }

    private fun timesPerDay(count: Int, amount: Quantity): List<() -> Schedule> {
        DEFAULT_TIMES[count]?.let { times -> return listOf { Schedule.EveryNDays(amount, 1, times) } }
        if (count <= 0 || Schedule.HOURS_IN_DAY % count != 0) return emptyList()
        val interval = Schedule.HOURS_IN_DAY / count
        if (interval !in SUPPORTED_HOUR_INTERVALS) return emptyList()
        return listOf { Schedule.EveryNHours(amount, interval, FIRST_DOSE) }
    }

    /**
     * Each digit is a number of doses at its slot. With a count unit the amount is that many of the
     * unit; without one it is the digit times the default dose, so `1-0-1` on a 50 mg label is 50 mg
     * morning and evening. One schedule per distinct non-zero digit.
     */
    private fun box(digits: List<Int>, countUnit: DoseUnit?, defaultDose: Quantity?): List<() -> Schedule> {
        val slots = digits.zip(BOX_SLOTS).filter { (digit, _) -> digit > 0 }
        return slots.groupBy({ it.first }, { it.second.slot }).mapNotNull { (digit, times) ->
            val amount = when {
                countUnit != null -> Quantity(BigDecimal(digit), countUnit)
                defaultDose != null -> Quantity(defaultDose.value.multiply(BigDecimal(digit)), defaultDose.unit)
                else -> return@mapNotNull null
            }
            val makeSchedule: () -> Schedule = { Schedule.EveryNDays(amount, 1, times.sorted()) }
            makeSchedule
        }
    }
}
