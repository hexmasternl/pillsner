package nl.hexmaster.pillsner.data

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import nl.hexmaster.pillsner.domain.model.LowStockAcknowledgement
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.repository.MedicationRepository

/**
 * A [MedicationRepository] held in memory. The app itself is wired with the Room-backed one; this
 * is what tests and Compose previews use, and it behaves the same from the outside.
 */
class InMemoryMedicationRepository(
    initial: List<Medication> = emptyList(),
) : MedicationRepository {

    private val medications = MutableStateFlow(initial)
    private val nextId = AtomicLong(initial.maxOfOrNull { it.id.value }?.plus(1) ?: 1L)

    override fun observeAll(): Flow<List<Medication>> = medications.asStateFlow()

    override suspend fun add(medication: NewMedication): MedicationId {
        val id = MedicationId(nextId.getAndIncrement())
        upsert(
            Medication(
                id = id,
                name = medication.name,
                defaultDose = medication.defaultDose,
                usedSince = medication.usedSince,
                useUntil = medication.useUntil,
                prescribedBy = medication.prescribedBy,
                schedules = medication.schedules,
                isActive = true,
            ),
        )
        return id
    }

    override suspend fun get(id: MedicationId): Medication? =
        medications.value.firstOrNull { it.id == id }

    override suspend fun update(medication: Medication) {
        val stored = checkNotNull(medications.value.firstOrNull { it.id == medication.id }) {
            "No medication with id ${medication.id.value}"
        }
        upsert(medication.copy(lowStockAcknowledgement = stored.lowStockAcknowledgement))
    }

    override suspend fun setActive(id: MedicationId, isActive: Boolean) {
        update(id) { it.copy(isActive = isActive) }
    }

    override suspend fun setLowStockAcknowledgement(id: MedicationId, value: LowStockAcknowledgement?) {
        update(id) { it.copy(lowStockAcknowledgement = value) }
    }

    /** Adds [medication], or replaces the one that already has its identifier. */
    fun upsert(medication: Medication) {
        medications.update { current ->
            val index = current.indexOfFirst { it.id == medication.id }
            if (index == -1) current + medication else current.toMutableList().also { it[index] = medication }
        }
    }

    /** Replaces the whole list in one emission. */
    fun replaceAll(medications: List<Medication>) {
        this.medications.value = medications
    }

    /** Replaces the medication with [id] by applying [transform] to it, if it is present. */
    fun update(id: MedicationId, transform: (Medication) -> Medication) {
        medications.update { current -> current.map { if (it.id == id) transform(it) else it } }
    }
}
