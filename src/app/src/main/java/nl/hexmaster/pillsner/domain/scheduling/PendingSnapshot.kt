package nl.hexmaster.pillsner.domain.scheduling

import java.time.Instant
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.repository.DoseRepository

/**
 * Every pending dose as of one point in a wake, together with each one's lapse moment, computed
 * once and shared by [DueDoses] and [ComputeWakeSchedule] instead of each re-querying
 * (reminder-wake-cycle-db-efficiency design D1).
 *
 * A lapse moment depends only on a dose's own [Dose.scheduledAt] and other doses' scheduled
 * moments — never on outcome, reminder or snooze fields — so [lapseAt] stays valid even after
 * [doses] itself is superseded by an updated copy that reflects a reminder having just been
 * posted.
 */
data class PendingSnapshot(
    val doses: List<Dose>,
    val lapseAt: Map<DoseId, Instant>,
) {
    /** [dose]'s lapse moment, resolved when this snapshot was built. */
    fun lapseAt(dose: Dose): Instant = lapseAt.getValue(dose.id)
}

/**
 * Builds a [PendingSnapshot] of every dose [doseRepository] currently has pending, resolving each
 * one's lapse moment through [markMissedDoses] once rather than leaving it to whichever use case
 * asks first.
 */
suspend fun buildPendingSnapshot(
    doseRepository: DoseRepository,
    markMissedDoses: MarkMissedDoses,
): PendingSnapshot {
    val doses = doseRepository.pending()
    val lapseAt = doses.associate { it.id to markMissedDoses.lapseAt(it) }
    return PendingSnapshot(doses, lapseAt)
}
