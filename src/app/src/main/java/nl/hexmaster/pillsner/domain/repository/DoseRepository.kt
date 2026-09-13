package nl.hexmaster.pillsner.domain.repository

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.PlannedDose

/** Storage of the doses the app has planned and of what became of them. */
interface DoseRepository {

    /** Every dose the user has not answered yet, ordered by scheduled moment, re-emitting on change. */
    fun observePending(): Flow<List<Dose>>

    /**
     * The pending doses from [from] onwards, soonest first, at most [limit] of them. Doses whose
     * moment has passed but that are still unanswered are included: they are still to be taken.
     */
    fun observeUpcoming(from: Instant, limit: Int): Flow<List<Dose>>

    /** Every pending dose, as one snapshot rather than a stream, for the reminder coordinator. */
    suspend fun pending(): List<Dose>

    /** Loads one dose, or null when it is not there. */
    suspend fun get(id: DoseId): Dose?

    /**
     * Stores every dose of [doses] that is not stored yet. A dose already present for the same
     * medication and moment is left exactly as it is, which is what makes refreshing idempotent.
     */
    suspend fun insertPlanned(doses: List<PlannedDose>)

    /**
     * Removes planned doses scheduled within [from]..[to] that are not in [keep], have no recorded
     * outcome and have never been reminded about. This is how a schedule edit, a deactivation or a
     * time zone change drops the doses that should no longer happen, without ever discarding a
     * dose the user has already seen or answered.
     */
    suspend fun deletePlannedNotIn(from: Instant, to: Instant, keep: Collection<Instant>)

    /** Records the outcome of one dose and clears any snooze on it. */
    suspend fun recordIntake(id: DoseId, outcome: IntakeOutcome, at: Instant)

    /** Sets, or with null clears, when a pending dose should be reminded about again. */
    suspend fun setSnooze(id: DoseId, until: Instant?)

    /** Records that the user has now been told about this dose for the first time. */
    suspend fun setFirstReminded(id: DoseId, at: Instant)

    /**
     * The moment of the next dose of [medicationId] after [after], or null when there is none in
     * the stored window. Used to decide when an unanswered dose is superseded.
     */
    suspend fun nextScheduledAtAfter(medicationId: MedicationId, after: Instant): Instant?
}
