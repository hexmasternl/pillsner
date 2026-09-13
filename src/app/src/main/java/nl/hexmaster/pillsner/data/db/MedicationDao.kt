package nl.hexmaster.pillsner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Reads and writes of medications and their schedules. */
@Dao
interface MedicationDao {

    /** Re-emits whenever a row in either table changes, which is what the overview lives on. */
    @Transaction
    @Query("SELECT * FROM medications")
    fun observeAllWithSchedules(): Flow<List<MedicationWithSchedules>>

    @Transaction
    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getWithSchedules(id: Long): MedicationWithSchedules?

    @Insert
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Insert
    suspend fun insertSchedules(schedules: List<ScheduleEntity>)

    @Query("UPDATE medications SET is_active = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)

    @Update
    suspend fun updateMedication(medication: MedicationEntity): Int

    @Query("DELETE FROM schedules WHERE medication_id = :medicationId")
    suspend fun deleteSchedulesFor(medicationId: Long)

    /**
     * Stores a medication and its schedules together. Room runs the body in one transaction, so a
     * schedule that cannot be inserted takes the medication row down with it and the user never
     * ends up with a medicine that has lost its schedules.
     *
     * @return the identifier the medication row was given.
     */
    @Transaction
    suspend fun insert(medication: MedicationEntity, schedules: List<ScheduleEntity>): Long {
        val medicationId = insertMedication(medication)
        insertSchedules(schedules.map { it.copy(medicationId = medicationId) })
        return medicationId
    }

    /**
     * Replaces a medication and every one of its schedules in one transaction.
     *
     * The schedule rows are thrown away and written again rather than diffed: a `Schedule` is a
     * value with no identity of its own, and nothing points at a schedule row — `doses` references
     * `medications`, never `schedules` — so replacing them cannot disturb a single dose. **Any
     * future table that does reference `schedules` has to revisit this method.**
     *
     * @throws IllegalStateException when no medication has that identifier, which rolls the whole
     *   transaction back.
     */
    @Transaction
    suspend fun update(medication: MedicationEntity, schedules: List<ScheduleEntity>) {
        check(updateMedication(medication) == 1) { "No medication row with id ${medication.id}" }
        deleteSchedulesFor(medication.id)
        insertSchedules(schedules.map { it.copy(medicationId = medication.id) })
    }
}
