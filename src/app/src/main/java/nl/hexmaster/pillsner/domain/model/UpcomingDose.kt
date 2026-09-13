package nl.hexmaster.pillsner.domain.model

import java.time.Instant

/** Identifies one planned dose. Stable for the life of the dose. */
@JvmInline
value class DoseId(val value: Long)

/**
 * A dose the user still has to answer, as the welcome screen needs it.
 *
 * "Upcoming" includes a dose whose moment has already passed but that the user has not answered:
 * it is still something they have to take. A dose that was taken, skipped or missed is not.
 *
 * @property doseId identity of the planned dose.
 * @property medicationName the medication's display name, as it was when the dose was planned.
 * @property amount how much this dose is.
 * @property scheduledAt the moment the dose is due, as an absolute instant. Presentation converts
 *   it to the device time zone; the domain never deals in local times.
 * @property isOverdue the scheduled moment has passed and the dose is still unanswered.
 * @property snoozedUntil when the user postponed the reminder to, or null.
 */
data class UpcomingDose(
    val doseId: DoseId,
    val medicationName: String,
    val amount: Quantity,
    val scheduledAt: Instant,
    val isOverdue: Boolean = false,
    val snoozedUntil: Instant? = null,
)
