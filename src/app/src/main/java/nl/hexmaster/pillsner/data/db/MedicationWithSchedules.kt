package nl.hexmaster.pillsner.data.db

import androidx.room.Embedded
import androidx.room.Relation

/** A medication row together with its schedule rows, as one read. */
data class MedicationWithSchedules(
    @Embedded val medication: MedicationEntity,
    @Relation(parentColumn = "id", entityColumn = "medication_id")
    val schedules: List<ScheduleEntity>,
)
