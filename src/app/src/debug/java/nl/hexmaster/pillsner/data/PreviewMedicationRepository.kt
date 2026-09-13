package nl.hexmaster.pillsner.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.repository.MedicationRepository

/**
 * Debug-only repository with five invented medicines, one per schedule shape and two inactive, so
 * the medicine overview can be seen with content in Compose previews and on a debug device.
 * Never wired into a release build.
 */
class PreviewMedicationRepository(
    private val medications: List<Medication> = sampleMedications,
) : MedicationRepository {

    override fun observeAll(): Flow<List<Medication>> = flowOf(medications)

    /** Previews never write; a preview that tried to save should fail visibly, not silently. */
    override suspend fun add(medication: NewMedication): MedicationId =
        error("PreviewMedicationRepository does not store medications")

    override suspend fun get(id: MedicationId): Medication? =
        medications.firstOrNull { it.id == id }

    override suspend fun update(medication: Medication): Unit =
        error("PreviewMedicationRepository does not change medications")

    override suspend fun setActive(id: MedicationId, isActive: Boolean): Unit =
        error("PreviewMedicationRepository does not change medications")

    companion object {
        private val startDate: LocalDate = LocalDate.of(2026, 9, 1)

        /** Invented names only; nothing here describes a real person's treatment. */
        val sampleMedications: List<Medication> = listOf(
            medication(
                id = 1,
                name = "Ibuprofen",
                defaultDose = Quantity.of("400", DoseUnit.MILLIGRAM),
                schedules = listOf(
                    Schedule.EveryNDays(
                        amount = Quantity.of("400", DoseUnit.MILLIGRAM),
                        intervalDays = 1,
                        times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                    ),
                ),
            ),
            medication(
                id = 2,
                name = "Amoxicillin",
                defaultDose = Quantity.of("500", DoseUnit.MILLIGRAM),
                schedules = listOf(
                    Schedule.EveryNHours(
                        amount = Quantity.of("500", DoseUnit.MILLIGRAM),
                        intervalHours = 8,
                        firstDoseAt = LocalTime.of(7, 0),
                    ),
                ),
            ),
            medication(
                id = 3,
                name = "paracetamol",
                defaultDose = Quantity.of("500", DoseUnit.MILLIGRAM),
                schedules = emptyList(),
            ),
            medication(
                id = 4,
                name = "Methotrexate",
                defaultDose = Quantity.of("1", DoseUnit.TABLET),
                schedules = listOf(
                    Schedule.OnWeekdays(
                        amount = Quantity.of("1", DoseUnit.TABLET),
                        days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                        times = listOf(LocalTime.of(9, 0)),
                    ),
                ),
                isActive = false,
            ),
            medication(
                id = 5,
                name = "Vitamin D",
                defaultDose = Quantity.of("2.5", DoseUnit.MILLILITRE),
                schedules = listOf(
                    Schedule.EveryNDays(
                        amount = Quantity.of("2.5", DoseUnit.MILLILITRE),
                        intervalDays = 2,
                        times = listOf(LocalTime.of(12, 0)),
                    ),
                ),
                isActive = false,
            ),
        )

        private fun medication(
            id: Long,
            name: String,
            defaultDose: Quantity,
            schedules: List<Schedule>,
            isActive: Boolean = true,
        ) = Medication(
            id = MedicationId(id),
            name = name,
            defaultDose = defaultDose,
            usedSince = startDate,
            useUntil = null,
            prescribedBy = Prescriber.GENERAL_PRACTITIONER,
            schedules = schedules,
            isActive = isActive,
        )
    }
}
