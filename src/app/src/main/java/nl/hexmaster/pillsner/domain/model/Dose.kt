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
 * @property intake the recorded outcome, or null while the dose is still pending.
 * @property snoozedUntil when a pending dose should be reminded about again, or null.
 * @property firstRemindedAt when the user was first told about this dose, or null while it is only
 *   planned. Planned doses may be regenerated freely; a dose the user has already seen is a fact
 *   and is never silently rewritten.
 */
data class Dose(
    val id: DoseId,
    val medicationId: MedicationId?,
    val medicationName: String,
    val amount: Quantity,
    val scheduledAt: Instant,
    val intake: Intake? = null,
    val snoozedUntil: Instant? = null,
    val firstRemindedAt: Instant? = null,
) {
    /** True while the user has not answered: no outcome has been recorded. */
    val isPending: Boolean get() = intake == null
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
