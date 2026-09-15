package nl.hexmaster.pillsner.domain.model

import java.time.Instant

/** What became of a dose. A skipped dose is a deliberate choice and is never a missed one. */
enum class IntakeOutcome {
    /** The user confirmed they took it. */
    TAKEN,

    /** The user deliberately did not take it. */
    SKIPPED,

    /** The user never answered and the dose lapsed. */
    MISSED,
}

/**
 * The recorded outcome of a dose.
 *
 * @property recordedAt for [IntakeOutcome.TAKEN] the moment the user confirmed it, which is what
 *   adherence is measured against; for the other outcomes the moment the outcome was settled.
 */
data class Intake(val outcome: IntakeOutcome, val recordedAt: Instant)

/**
 * One planned intake of a medication, and what became of it.
 *
 * A dose carries a **snapshot** of the medicine's name and amount as they were when it was
 * generated. Renaming a medicine or changing its dose next week must not rewrite what the user
 * took last Tuesday, and a dose outlives its medication ([medicationId] becomes null) so history
 * is never lost.
 *
 * @property medicationId the medication this came from, or null once that medication is gone.
 * @property medicationName the medicine's name when this dose was planned.
 * @property amount how much this dose is; the schedule's amount when it was planned.
 * @property scheduledAt the moment the dose is due, as an absolute instant.
 * @property plannedAt the moment this dose was first stored, which is never rewritten. A dose
 *   planned while it was still in the future had a window in which to remind; one generated after
 *   its own moment — a medicine saved in the evening with a morning schedule — never did. That is
 *   the difference between a reminder the platform dropped and one that was never possible
 *   (design D4).
 * @property intake the recorded outcome, or null while the dose is still pending.
 * @property snoozedUntil when a pending dose should be reminded about again, or null.
 * @property firstRemindedAt when the user was first told about this dose, or null while it is only
 *   planned. Planned doses may be regenerated freely; a dose the user has already seen is a fact
 *   and is never silently rewritten.
 * @property lastRemindedAt when the reminder for this dose was last posted, or null while it has
 *   never been. This is the moment the repeat rule counts its quarter of an hour from, which is
 *   why it moves with every posting while [firstRemindedAt] stays where it was.
 * @property reminderCount how often the reminder has been asked again since it was first
 *   announced. Stored rather than held in memory, because the repeat rule has to survive the
 *   process dying between one repeat and the next. A snooze resets it to nought: the user has
 *   acknowledged the dose, so the app does not go on counting down against them.
 */
data class Dose(
    val id: DoseId,
    val medicationId: MedicationId?,
    val medicationName: String,
    val amount: Quantity,
    val scheduledAt: Instant,
    val plannedAt: Instant = Instant.EPOCH,
    val intake: Intake? = null,
    val snoozedUntil: Instant? = null,
    val firstRemindedAt: Instant? = null,
    val lastRemindedAt: Instant? = null,
    val reminderCount: Int = 0,
) {
    /** True while the user has not answered: no outcome has been recorded. */
    val isPending: Boolean get() = intake == null

    /**
     * True when this dose was recorded missed without the user ever having been told about it,
     * although the app had a window in which to tell them (design D2, D4).
     *
     * That is the observable symptom of an alarm the platform did not deliver, and it is the only
     * one the app has: `AlarmManager` will not say what it holds, so the consequence is what can be
     * seen. Two things keep it honest. The dose must carry the missed outcome, so a dose still
     * pending or one the user answered never counts. And [plannedAt] must be before [scheduledAt],
     * so a dose generated after its own moment — a medicine saved in the evening with a morning
     * schedule — is not read as a reminder that went astray when there was never one to give.
     *
     * Whether notifications were allowed is not part of this: the dose does not know, and a dose
     * that went unannounced because the user denied the permission has its own banner. The caller
     * checks that before recording anything.
     */
    val wasMissedInSilence: Boolean
        get() = intake?.outcome == IntakeOutcome.MISSED &&
            firstRemindedAt == null &&
            plannedAt.isBefore(scheduledAt)
}

/**
 * A dose the generator says should exist, before it has been stored.
 *
 * Two schedules of one medicine that fall on the same minute produce one planned dose, keeping the
 * first schedule's amount; the schedule editor already steers users away from that overlap.
 */
data class PlannedDose(
    val medicationId: MedicationId,
    val medicationName: String,
    val amount: Quantity,
    val scheduledAt: Instant,
)
