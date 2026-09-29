package nl.hexmaster.pillsner.domain.labelscan

import java.time.LocalDate

/**
 * Turns the lines read off a medicine label into the fields of the Add medicine form
 * (medicine-label-photo-prefill design D4).
 *
 * Pure Kotlin: nothing here knows about Android, the camera or the recogniser, so every rule is
 * unit-tested per language. It is deterministic and cheap enough to run on every camera frame.
 * A field the rules cannot settle is absent, never guessed; the form's ordinary validation still
 * runs on whatever is filled in.
 *
 * The rules, in order (design D4):
 * 1. Dose tokens: every `<number><unit>` becomes a quantity; mass and volume units are strengths,
 *    form units are counts.
 * 2. Default dose: the first strength, otherwise the first count on an instruction line.
 * 3. Schedule amount: the count on the instruction line, otherwise a strength or volume on that
 *    same line, otherwise the default dose.
 * 4. Frequency to schedule, through the app's own schedule shapes; anything else gives none.
 * 5. Name: the first non-noise line that shares a line with a strength, otherwise the first.
 * 6. Used since: the most recent past date within a year, otherwise today.
 * 7. Use until: an until-date, otherwise a course length counted from used since, otherwise none.
 */
class InterpretLabelText {

    /**
     * @param lines the recognised lines in reading order.
     * @param today the date the scan happens on, so the rules never read the clock themselves.
     */
    operator fun invoke(lines: List<RecognisedLine>, today: LocalDate): LabelInterpretation {
        val texts = lines.map { it.text.trim() }.filter { it.isNotEmpty() }
        if (texts.isEmpty()) return LabelInterpretation.empty(today)

        val normalised = texts.map(LabelVocabulary::normalise)
        val tokens = normalised.map(DoseTokens::extract)
        val frequencies = normalised.map(LabelFrequency::detect)
        val isInstruction = frequencies.map { it != null }

        // Rules 1 and 2.
        val strength = tokens.flatten().firstOrNull { it.isStrength }?.quantity
        // A line with a range ("1-2 tablets") states an amount the form has no field for; neither
        // of its numbers is the dose, so the line lends no count and gets no schedule.
        val hasRange = normalised.map { AMOUNT_RANGE.containsMatchIn(it) }
        val instructionCount = normalised.indices
            .filter { isInstruction[it] && !hasRange[it] }
            .flatMap { tokens[it] }
            .firstOrNull { !it.isStrength }
            ?.quantity
        val defaultDose = strength ?: instructionCount

        // Rules 3 and 4: the first instruction line decides. Its own count ("1 tablet") is the
        // amount; failing that its own strength or volume ("10 ml"), which outranks a strength
        // printed elsewhere on the label ("125 mg/5 ml"); failing both, the default dose.
        val instructionIndex = frequencies.indexOfFirst { it != null }
        val schedules = if (instructionIndex < 0) {
            emptyList()
        } else {
            val lineTokens = tokens[instructionIndex]
            val lineAmount = (lineTokens.firstOrNull { !it.isStrength } ?: lineTokens.firstOrNull { it.isStrength })?.quantity
            // "1-2 tablets": a range the form cannot hold. Neither its upper number nor the label's
            // strength is what the label said, so the schedule is left absent for the user.
            if (hasRange[instructionIndex]) {
                emptyList()
            } else {
                LabelSchedules.build(
                    frequency = frequencies[instructionIndex]!!,
                    amount = lineAmount ?: defaultDose,
                    countUnit = instructionCount?.unit ?: LabelFrequency.bareFormUnit(normalised[instructionIndex]),
                    defaultDose = defaultDose,
                )
            }
        }

        // Rule 5.
        val name = LabelName.choose(texts)

        // Rules 6 and 7. Dates on expiry or lot lines are when the medicine goes off, never a start
        // or an end of use, so they are left out of both.
        val dateLines = normalised.filterNot(LabelDates::isExpiryLine)
        val untilDates = dateLines.flatMap(LabelDates::untilDates)
        val allDates = dateLines.flatMap { line -> LabelDates.candidates(line).map { it.date } }
        val usedSince = LabelDates.usedSince(allDates, untilDates.toSet(), today)
        val useUntil = LabelDates.useUntil(normalised, isInstruction, usedSince, untilDates)

        return LabelInterpretation(
            name = name,
            defaultDose = defaultDose,
            schedules = schedules,
            usedSince = usedSince,
            useUntil = useUntil,
            rawText = texts.joinToString("\n"),
        )
    }

    private companion object {
        /**
         * "1-2 tablets", "1 to 2 tablets", "1 à 2 comprimés": an amount the form has no field for.
         * Not the box notation "2-0-1 Tabletten": a range is exactly two numbers, so the first may
         * not follow a dash and the second may not precede one.
         */
        val AMOUNT_RANGE = Regex(
            "(?<!-\\s{0,2})\\b\\d+(?:[.,]\\d+)?\\s*(?:-|–|\\bto\\b|\\btot\\b|\\bbis\\b|\\ba\\b|\\bà\\b|\\bou\\b|\\bo\\b|\\bor\\b|\\bof\\b|\\boder\\b)\\s*" +
                "\\d+(?:[.,]\\d+)?(?!\\s*-\\s*\\d)\\s+(?:" + LabelVocabulary.alternation(LabelVocabulary.units.keys) + ")\\b",
        )
    }
}
