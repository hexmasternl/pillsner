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
 * 3. Schedule amount: the count on the instruction line, otherwise the default dose.
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
        val instructionCount = normalised.indices
            .filter { isInstruction[it] }
            .flatMap { tokens[it] }
            .firstOrNull { !it.isStrength }
            ?.quantity
        val defaultDose = strength ?: instructionCount

        // Rules 3 and 4: the first instruction line decides.
        val instructionIndex = frequencies.indexOfFirst { it != null }
        val schedules = if (instructionIndex < 0) {
            emptyList()
        } else {
            val lineCount = tokens[instructionIndex].firstOrNull { !it.isStrength }?.quantity
            LabelSchedules.build(
                frequency = frequencies[instructionIndex]!!,
                amount = lineCount ?: defaultDose,
                countUnit = instructionCount?.unit ?: LabelFrequency.bareFormUnit(normalised[instructionIndex]),
                defaultDose = defaultDose,
            )
        }

        // Rule 5.
        val name = LabelName.choose(texts)

        // Rules 6 and 7.
        val untilDates = normalised.flatMap(LabelDates::untilDates)
        val allDates = normalised.flatMap { line -> LabelDates.candidates(line).map { it.date } }
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
}
