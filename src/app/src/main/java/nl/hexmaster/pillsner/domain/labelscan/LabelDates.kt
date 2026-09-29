package nl.hexmaster.pillsner.domain.labelscan

import java.time.DateTimeException
import java.time.LocalDate

/** Reads dates and course lengths off the label, conservatively (design D4 rules 6 and 7). */
internal object LabelDates {

    /** A date found on a line and where it sits, so "until" can tell which date follows it. */
    data class DateMatch(val date: LocalDate, val range: IntRange)

    /**
     * Every numeric date on a normalised line: `dd-mm-yyyy`, `dd/mm/yyyy`, `dd.mm.yyyy`,
     * `yyyy-mm-dd`, and the same with a two-digit year. A month-and-year form such as `03/2028` is
     * never a candidate: that is how expiry dates are printed.
     */
    fun candidates(normalised: String): List<DateMatch> {
        val dayFirst = DAY_MONTH_YEAR.findAll(normalised).mapNotNull { match ->
            val (day, month, year) = match.destructured
            date(year, month, day)?.let { DateMatch(it, match.range) }
        }
        val yearFirst = YEAR_MONTH_DAY.findAll(normalised).mapNotNull { match ->
            val (year, month, day) = match.destructured
            date(year, month, day)?.let { DateMatch(it, match.range) }
        }
        return (dayFirst + yearFirst).sortedBy { it.range.first }.toList()
    }

    /**
     * Whether the line is an expiry or lot line ("EXP 03/2028", "houdbaar tot 15-10-2026"). Its date
     * is when the medicine goes off, not when its use starts or ends, so it is no candidate for
     * either.
     */
    fun isExpiryLine(normalised: String): Boolean =
        LabelVocabulary.words(normalised).any { it in LabelVocabulary.expiryWords } || EXPIRY_PHRASE.containsMatchIn(normalised)

    /** Whether the line is a date and nothing else, which makes it noise for the name. */
    fun isOnlyDate(normalised: String): Boolean {
        val matches = candidates(normalised)
        if (matches.isEmpty()) return false
        val rest = matches.fold(normalised) { text, match -> text.replaceRange(match.range, " ".repeat(match.range.count())) }
        return rest.none { it.isLetterOrDigit() }
    }

    /** The dates that follow an "until" word on the line, in order. */
    fun untilDates(normalised: String): List<LocalDate> {
        val dates = candidates(normalised)
        return UNTIL.findAll(normalised).mapNotNull { until ->
            dates.firstOrNull { it.range.first > until.range.last && it.range.first - until.range.last <= UNTIL_REACH }?.date
        }.toList()
    }

    /**
     * The most recent date on or before [today] and at most a year before it, excluding [untilDates];
     * [today] when there is none. A date after today is an expiry or an end, never a start.
     */
    fun usedSince(allDates: List<LocalDate>, untilDates: Set<LocalDate>, today: LocalDate): LocalDate =
        allDates.filter { it !in untilDates && !it.isAfter(today) && !it.isBefore(today.minusDays(MAX_AGE_DAYS)) }
            .maxOrNull() ?: today

    /**
     * The end of use, in priority order: an until-date after [usedSince]; otherwise a course of N
     * days or weeks, counted inclusively from [usedSince]; otherwise null.
     *
     * A duration counts when it follows a "for/during" word or is followed by "lang", on any line,
     * or when it stands alone on an instruction line.
     */
    fun useUntil(
        normalisedLines: List<String>,
        isInstructionLine: List<Boolean>,
        usedSince: LocalDate,
        untilDates: List<LocalDate>,
    ): LocalDate? {
        untilDates.firstOrNull { it.isAfter(usedSince) }?.let { return it }
        normalisedLines.forEachIndexed { index, line ->
            val duration = DURATION_AFTER_FOR.find(line) ?: DURATION_BEFORE_LANG.find(line)
                ?: if (isInstructionLine[index]) DURATION_ALONE.find(line) else null
            if (duration != null) {
                val count = LabelFrequency.number(duration.groupValues[1]) ?: return@forEachIndexed
                if (count <= 0) return@forEachIndexed
                val days = if (WEEK_WORD.matches(duration.groupValues[2])) count * DAYS_IN_WEEK else count
                val end = usedSince.plusDays(days - 1L)
                return end.takeUnless { it.isBefore(usedSince) }
            }
        }
        return null
    }

    private fun date(year: String, month: String, day: String): LocalDate? {
        val fullYear = year.toInt().let { if (year.length == 2) TWO_DIGIT_YEAR_BASE + it else it }
        return try {
            LocalDate.of(fullYear, month.toInt(), day.toInt())
        } catch (invalid: DateTimeException) {
            null
        }
    }

    private const val TWO_DIGIT_YEAR_BASE = 2000
    private const val MAX_AGE_DAYS = 365L
    private const val DAYS_IN_WEEK = 7

    /** How far after an "until" word a date may sit and still belong to it: "tot en met de 15-10-2026". */
    private const val UNTIL_REACH = 12

    private val DAY_MONTH_YEAR = Regex("(?<![\\d./-])(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4}|\\d{2})(?![\\d./-])")
    private val YEAR_MONTH_DAY = Regex("(?<![\\d./-])(\\d{4})-(\\d{1,2})-(\\d{1,2})(?![\\d./-])")

    /** Expiry phrases that no single word gives away: "use by", "niet gebruiken na", "à utiliser avant". */
    private val EXPIRY_PHRASE = Regex(
        "\\b(?:" + LabelVocabulary.alternation(
            listOf(
                "use by", "best before", "do not use after", "niet gebruiken na", "te gebruiken tot", "ten minste houdbaar tot",
                "mindestens haltbar bis", "nicht verwenden nach", "a utiliser avant", "ne pas utiliser apres",
                "consumir antes", "no usar despues", "valido ate", "nao usar apos",
            ),
        ) + ")\\b",
    )

    private val UNTIL = Regex(
        "\\b(?:" + LabelVocabulary.alternation(
            listOf(
                "tot en met", "tot", "t/m", "until", "till", "up to", "through", "bis zum", "bis", "jusqu'au",
                "jusqu au", "jusqu'a", "hasta el", "hasta", "ate o", "ate",
            ),
        ) + ")\\b",
    )

    private val NUMBER = "(\\d+|" + LabelVocabulary.alternation(LabelVocabulary.numberWords.keys) + ")"
    private val DAY_WORDS = LabelVocabulary.alternation(listOf("dagen", "dag", "days", "day", "tage", "tagen", "tag", "jours", "jour", "dias", "dia"))
    private val WEEK_WORDS = LabelVocabulary.alternation(listOf("weken", "week", "weeks", "wochen", "woche", "semaines", "semaine", "semanas", "semana"))
    private val PERIOD = "($DAY_WORDS|$WEEK_WORDS)"
    /**
     * Words that introduce a course length. English "per" is deliberately absent: "1 tablet per
     * 7 days" is a rhythm, and an unsupported one, not a seven-day course. Spanish and Portuguese
     * "por 7 días" is a course and stays.
     */
    private val FOR_WORDS = LabelVocabulary.alternation(listOf("gedurende", "voor", "for", "fur", "wahrend", "pendant", "durant", "durante", "por"))
    private val EVERY_WORDS = LabelVocabulary.alternation(listOf("om de", "elke", "iedere", "every", "each", "alle", "jede", "jeden", "tous les", "toutes les", "chaque", "cada", "a cada"))

    private val DURATION_AFTER_FOR = Regex("\\b(?:$FOR_WORDS)\\s+(?:de\\s+|het\\s+)?$NUMBER\\s*$PERIOD\\b")
    private val DURATION_BEFORE_LANG = Regex("\\b$NUMBER\\s*$PERIOD\\s+lang\\b")

    /** A bare "7 dagen", but not the "2" of "alle 2 Tage", which is a rhythm rather than a length. */
    private val DURATION_ALONE = Regex("(?<!\\b(?:$EVERY_WORDS)\\s)\\b$NUMBER\\s*$PERIOD\\b")
    private val WEEK_WORD = Regex(WEEK_WORDS)
}
