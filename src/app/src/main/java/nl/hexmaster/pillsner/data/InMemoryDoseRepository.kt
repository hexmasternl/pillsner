package nl.hexmaster.pillsner.data

import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.Intake
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.PlannedDose
import nl.hexmaster.pillsner.domain.repository.DoseRepository

/**
 * A [DoseRepository] held in memory, with the same rules as the Room one: planned inserts ignore a
 * dose that is already there, and only unanswered, un-reminded doses can be withdrawn. Used by the
 * unit tests of the scheduling use cases and by Compose previews.
 */
class InMemoryDoseRepository(initial: List<Dose> = emptyList()) : DoseRepository {

    private val doses = MutableStateFlow(initial)
    private val nextId = AtomicLong(initial.maxOfOrNull { it.id.value }?.plus(1) ?: 1L)

    override fun observePending(): Flow<List<Dose>> =
        doses.map { all -> all.filter { it.isPending }.sortedBy { dose -> dose.scheduledAt } }

    override fun observeUpcoming(from: Instant, limit: Int): Flow<List<Dose>> =
        observePending().map { it.take(limit) }

    override suspend fun pending(): List<Dose> =
        doses.value.filter { it.isPending }.sortedBy { it.scheduledAt }

    override suspend fun get(id: DoseId): Dose? = doses.value.firstOrNull { it.id == id }

    override suspend fun insertPlanned(doses: List<PlannedDose>) {
        this.doses.update { current ->
            val known = current.map { it.medicationId to it.scheduledAt }.toSet()
            current + doses
                .filterNot { (it.medicationId to it.scheduledAt) in known }
                .map { planned ->
                    Dose(
                        id = DoseId(nextId.getAndIncrement()),
                        medicationId = planned.medicationId,
                        medicationName = planned.medicationName,
                        amount = planned.amount,
                        scheduledAt = planned.scheduledAt,
                    )
                }
        }
    }

    override suspend fun deletePlannedNotIn(from: Instant, to: Instant, keep: Collection<Instant>) {
        val kept = keep.toSet()
        doses.update { current ->
            current.filterNot { dose ->
                dose.isPending &&
                    dose.firstRemindedAt == null &&
                    !dose.scheduledAt.isBefore(from) &&
                    dose.scheduledAt.isBefore(to) &&
                    dose.scheduledAt !in kept
            }
        }
    }

    override suspend fun recordIntake(id: DoseId, outcome: IntakeOutcome, at: Instant) {
        update(id) { it.copy(intake = Intake(outcome, at), snoozedUntil = null) }
    }

    override suspend fun setSnooze(id: DoseId, until: Instant?) {
        update(id) { it.copy(snoozedUntil = until) }
    }

    override suspend fun setFirstReminded(id: DoseId, at: Instant) {
        update(id) { it.copy(firstRemindedAt = at) }
    }

    override suspend fun nextScheduledAtAfter(medicationId: MedicationId, after: Instant): Instant? =
        doses.value
            .filter { it.medicationId == medicationId && it.scheduledAt.isAfter(after) }
            .minOfOrNull { it.scheduledAt }

    override fun observeHistoryFor(medicationId: MedicationId, from: Instant, to: Instant): Flow<List<Dose>> =
        doses.map { all ->
            all.filter {
                it.medicationId == medicationId &&
                    !it.scheduledAt.isBefore(from) &&
                    it.scheduledAt.isBefore(to)
            }.sortedBy { it.scheduledAt }
        }

    override suspend fun earliestScheduledAt(medicationId: MedicationId): Instant? =
        doses.value.filter { it.medicationId == medicationId }.minOfOrNull { it.scheduledAt }

    /** Everything stored, answered doses included, for assertions. */
    fun all(): List<Dose> = doses.value.sortedBy { it.scheduledAt }

    private fun update(id: DoseId, transform: (Dose) -> Dose) {
        doses.update { current -> current.map { if (it.id == id) transform(it) else it } }
    }
}
