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
import nl.hexmaster.pillsner.domain.repository.ReminderOutcomeUpdate

/**
 * A [DoseRepository] held in memory, with the same rules as the Room one: planned inserts ignore a
 * dose that is already there, a pending dose's name and amount follow its medicine, and a dose is
 * withdrawn only when its own medicine no longer plans its moment. Used by the unit tests of the
 * scheduling use cases and by Compose previews.
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

    override fun observe(id: DoseId): Flow<Dose?> = doses.map { all -> all.firstOrNull { it.id == id } }

    override suspend fun insertPlanned(doses: List<PlannedDose>, plannedAt: Instant) {
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
                        plannedAt = plannedAt,
                    )
                }
        }
    }

    override suspend fun refreshSnapshots(doses: List<PlannedDose>) {
        val bySlot = doses.associateBy { it.medicationId to it.scheduledAt }
        this.doses.update { current ->
            current.map { dose ->
                val planned = bySlot[dose.medicationId to dose.scheduledAt]
                if (planned == null || !dose.isPending) {
                    dose
                } else {
                    dose.copy(medicationName = planned.medicationName, amount = planned.amount)
                }
            }
        }
    }

    override suspend fun withdrawPlanned(
        from: Instant,
        to: Instant,
        planned: Map<MedicationId, List<Instant>>,
        includeReminded: Boolean,
    ): List<DoseId> {
        val moments = planned.mapValues { (_, at) -> at.toSet() }

        val withdrawn = doses.value.filter { dose ->
            val medicationId = dose.medicationId
            dose.isPending &&
                (includeReminded || dose.firstRemindedAt == null) &&
                !dose.scheduledAt.isBefore(from) &&
                dose.scheduledAt.isBefore(to) &&
                // A dose whose medicine is gone belongs to no plan and is left alone.
                medicationId != null &&
                medicationId in moments &&
                dose.scheduledAt !in moments.getValue(medicationId)
        }

        if (withdrawn.isEmpty()) return emptyList()

        val ids = withdrawn.map { it.id }.toSet()
        doses.update { current -> current.filterNot { it.id in ids } }
        return withdrawn.map { it.id }
    }

    override suspend fun recordIntake(id: DoseId, outcome: IntakeOutcome, at: Instant) {
        update(id) { it.copy(intake = Intake(outcome, at), snoozedUntil = null) }
    }

    override suspend fun setSnooze(id: DoseId, until: Instant?) {
        // A snooze is an acknowledgement, so it also puts the repeat sequence back to the start.
        update(id) { it.copy(snoozedUntil = until, reminderCount = 0) }
    }

    override suspend fun recordReminded(id: DoseId, at: Instant, countsAsRepeat: Boolean) {
        update(id) {
            it.copy(
                firstRemindedAt = it.firstRemindedAt ?: at,
                lastRemindedAt = at,
                reminderCount = it.reminderCount + if (countsAsRepeat) 1 else 0,
            )
        }
    }

    override suspend fun applyReminderOutcomes(updates: List<ReminderOutcomeUpdate>) {
        updates.forEach { update ->
            recordReminded(update.id, update.at, update.countsAsRepeat)
            if (update.clearsSnooze) setSnooze(update.id, null)
        }
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

    override suspend fun deleteHistoryBefore(cutoff: Instant): Int {
        val before = doses.value.size
        doses.update { current -> current.filterNot { it.scheduledAt.isBefore(cutoff) } }
        return before - doses.value.size
    }

    override suspend fun hasAnyDose(): Boolean = doses.value.isNotEmpty()

    override suspend fun latestKnownMoment(): Instant? = doses.value.maxOfOrNull { it.plannedAt }

    /** Everything stored, answered doses included, for assertions. */
    fun all(): List<Dose> = doses.value.sortedBy { it.scheduledAt }

    private fun update(id: DoseId, transform: (Dose) -> Dose) {
        doses.update { current -> current.map { if (it.id == id) transform(it) else it } }
    }
}
