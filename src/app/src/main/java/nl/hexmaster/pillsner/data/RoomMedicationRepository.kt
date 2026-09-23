package nl.hexmaster.pillsner.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.hexmaster.pillsner.data.db.MedicationDao
import nl.hexmaster.pillsner.data.db.toDomain
import nl.hexmaster.pillsner.data.db.toEntity
import nl.hexmaster.pillsner.domain.model.LowStockAcknowledgement
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.repository.MedicationRepository

/** The wired [MedicationRepository]: everything the user enters, kept on the device by Room. */
class RoomMedicationRepository(
    private val dao: MedicationDao,
) : MedicationRepository {

    override fun observeAll(): Flow<List<Medication>> =
        dao.observeAllWithSchedules().map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(medication: NewMedication): MedicationId {
        val schedules = medication.schedules.mapIndexed { position, schedule ->
            schedule.toEntity(medicationId = 0L, position = position)
        }
        return MedicationId(dao.insert(medication.toEntity(), schedules))
    }

    override suspend fun get(id: MedicationId): Medication? =
        dao.getWithSchedules(id.value)?.toDomain()

    override suspend fun update(medication: Medication) {
        val schedules = medication.schedules.mapIndexed { position, schedule ->
            schedule.toEntity(medicationId = medication.id.value, position = position)
        }
        dao.update(medication.toEntity(), schedules)
    }

    override suspend fun setActive(id: MedicationId, isActive: Boolean) {
        dao.setActive(id.value, isActive)
    }

    override suspend fun setLowStockAcknowledgement(id: MedicationId, value: LowStockAcknowledgement?) {
        dao.setLowStockAcknowledgement(id.value, value?.name)
    }
}
