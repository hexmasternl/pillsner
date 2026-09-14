package nl.hexmaster.pillsner.data

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.hexmaster.pillsner.data.db.DoseDao
import nl.hexmaster.pillsner.data.db.DoseEntity
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Intake
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.PlannedDose
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.repository.DoseRepository

/** The wired [DoseRepository]: every planned dose and every answer, kept on the device by Room. */
class RoomDoseRepository(
    private val dao: DoseDao,
) : DoseRepository {

    override fun observePending(): Flow<List<Dose>> =
        dao.observePending().map { rows -> rows.map { it.toDomain() } }

    override fun observeUpcoming(from: Instant, limit: Int): Flow<List<Dose>> =
        dao.observeUpcoming(UNTIL_THE_END_OF_THE_WINDOW, limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun pending(): List<Dose> = dao.pending().map { it.toDomain() }

    override suspend fun get(id: DoseId): Dose? = dao.get(id.value)?.toDomain()

    override suspend fun insertPlanned(doses: List<PlannedDose>) {
        if (doses.isEmpty()) return
        dao.insertIgnore(doses.map { it.toEntity() })
    }

    override suspend fun refreshSnapshots(doses: List<PlannedDose>) {
        doses.forEach {
            dao.refreshSnapshot(
                medicationId = it.medicationId.value,
                scheduledAt = it.scheduledAt,
                name = it.medicationName,
                amountValue = it.amount.value,
                amountUnit = it.amount.unit.name,
            )
        }
    }

    override suspend fun withdrawPlanned(
        from: Instant,
        to: Instant,
        planned: Map<MedicationId, List<Instant>>,
        includeReminded: Boolean,
    ): List<DoseId> {
        val (scheduled, unscheduled) = planned.entries.partition { it.value.isNotEmpty() }

        // Read the ids first, then delete them: a delete cannot say what it removed, and the
        // coordinator needs the list to take down the notifications for them.
        val ids = buildList {
            scheduled.forEach { (medicationId, moments) ->
                addAll(
                    dao.plannedNoLongerScheduled(
                        medicationId = medicationId.value,
                        from = from,
                        to = to,
                        keep = moments,
                        includeReminded = includeReminded,
                    ),
                )
            }
            if (unscheduled.isNotEmpty()) {
                addAll(
                    dao.plannedForUnscheduledMedications(
                        medicationIds = unscheduled.map { it.key.value },
                        from = from,
                        to = to,
                        includeReminded = includeReminded,
                    ),
                )
            }
        }

        if (ids.isEmpty()) return emptyList()
        dao.deleteByIds(ids)
        return ids.map(::DoseId)
    }

    override suspend fun recordIntake(id: DoseId, outcome: IntakeOutcome, at: Instant) {
        dao.setIntake(id.value, outcome.name, at)
    }

    override suspend fun setSnooze(id: DoseId, until: Instant?) {
        dao.setSnooze(id.value, until)
    }

    override suspend fun setFirstReminded(id: DoseId, at: Instant) {
        dao.setFirstReminded(id.value, at)
    }

    override suspend fun nextScheduledAtAfter(medicationId: MedicationId, after: Instant): Instant? =
        dao.nextScheduledAtAfter(medicationId.value, after)

    override fun observeHistoryFor(medicationId: MedicationId, from: Instant, to: Instant): Flow<List<Dose>> =
        dao.observeHistoryFor(medicationId.value, from, to).map { rows -> rows.map { it.toDomain() } }

    override suspend fun earliestScheduledAt(medicationId: MedicationId): Instant? =
        dao.earliestScheduledAt(medicationId.value)

    private companion object {
        /**
         * The planning window never reaches beyond tomorrow, so "everything pending" and
         * "everything pending up to some far moment" are the same list; a far bound keeps the
         * query one statement.
         */
        val UNTIL_THE_END_OF_THE_WINDOW: Instant = Instant.ofEpochMilli(Long.MAX_VALUE / 2)
    }
}

internal fun DoseEntity.toDomain(): Dose = Dose(
    id = DoseId(id),
    medicationId = medicationId?.let(::MedicationId),
    medicationName = medicationName,
    amount = Quantity(amountValue, DoseUnit.valueOf(amountUnit)),
    scheduledAt = scheduledAt,
    intake = outcome?.let { Intake(IntakeOutcome.valueOf(it), checkNotNull(recordedAt) { "Dose $id has an outcome but no moment" }) },
    snoozedUntil = snoozedUntil,
    firstRemindedAt = firstRemindedAt,
)

internal fun PlannedDose.toEntity(): DoseEntity = DoseEntity(
    medicationId = medicationId.value,
    medicationName = medicationName,
    amountValue = amount.value,
    amountUnit = amount.unit.name,
    scheduledAt = scheduledAt,
)
