package nl.hexmaster.pillsner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Reads and writes of planned doses and their outcomes. */
@Dao
interface DoseDao {

    @Query("SELECT * FROM doses WHERE outcome IS NULL ORDER BY scheduled_at ASC")
    fun observePending(): Flow<List<DoseEntity>>

    @Query(
        """
        SELECT * FROM doses
        WHERE outcome IS NULL AND scheduled_at <= :until
        ORDER BY scheduled_at ASC
        LIMIT :limit
        """,
    )
    fun observeUpcoming(until: Instant, limit: Int): Flow<List<DoseEntity>>

    @Query("SELECT * FROM doses WHERE outcome IS NULL ORDER BY scheduled_at ASC")
    suspend fun pending(): List<DoseEntity>

    @Query("SELECT * FROM doses WHERE id = :id")
    suspend fun get(id: Long): DoseEntity?

    /**
     * Adds only the doses that are not stored yet: the unique index on medication and moment turns
     * a re-plan of the same window into a no-op, which is what makes refreshing idempotent.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(doses: List<DoseEntity>)

    /**
     * The pending doses of one medicine, inside a window, that its schedules no longer call for.
     *
     * Matching is on the medicine *and* the moment. Another medicine planning a dose at the same
     * instant must never keep this one alive, which is what went wrong when the window was matched
     * on the moment alone.
     *
     * [includeReminded] is the difference between an edit and a clock change. When the user has
     * just changed this medicine they have said they no longer take it then, so even a dose the app
     * has already reminded about is withdrawn. A clock or time-zone change may not do that:
     * a dose the user has already been told about keeps its moment.
     *
     * A dose with an outcome is the user's record and is never selected.
     */
    @Query(
        """
        SELECT id FROM doses
        WHERE outcome IS NULL
          AND medication_id = :medicationId
          AND scheduled_at >= :from AND scheduled_at < :to
          AND scheduled_at NOT IN (:keep)
          AND (:includeReminded OR first_reminded_at IS NULL)
        """,
    )
    suspend fun plannedNoLongerScheduled(
        medicationId: Long,
        from: Instant,
        to: Instant,
        keep: Collection<Instant>,
        includeReminded: Boolean,
    ): List<Long>

    /**
     * The pending doses, inside a window, of the medicines in [medicationIds] — the ones that now
     * plan nothing at all, because they were deactivated, lost their last schedule, or fell outside
     * the days they are used.
     *
     * A dose whose medicine has gone from the database has a null reference and is never selected:
     * it is the user's own record of something that no longer exists.
     */
    @Query(
        """
        SELECT id FROM doses
        WHERE outcome IS NULL
          AND medication_id IN (:medicationIds)
          AND scheduled_at >= :from AND scheduled_at < :to
          AND (:includeReminded OR first_reminded_at IS NULL)
        """,
    )
    suspend fun plannedForUnscheduledMedications(
        medicationIds: Collection<Long>,
        from: Instant,
        to: Instant,
        includeReminded: Boolean,
    ): List<Long>

    /** Withdraws the doses the two queries above found. */
    @Query("DELETE FROM doses WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: Collection<Long>)

    /**
     * Brings one pending dose's name and amount up to date with the medicine it belongs to.
     *
     * The snapshot columns exist so a dose stays readable when its medicine is gone, but while the
     * dose is still pending they must follow the medicine: a reminder that names a medicine the
     * user has just renamed is worse than no snapshot at all. A dose with an outcome is history and
     * keeps what it was recorded with.
     *
     * The last clause keeps a refresh that changes nothing from touching a row, so the dose stream
     * only re-emits when something a screen shows has actually moved.
     */
    @Query(
        """
        UPDATE doses
        SET medication_name = :name, amount_value = :amountValue, amount_unit = :amountUnit
        WHERE outcome IS NULL
          AND medication_id = :medicationId
          AND scheduled_at = :scheduledAt
          AND (
            medication_name <> :name
              OR amount_value <> :amountValue
              OR amount_unit <> :amountUnit
          )
        """,
    )
    suspend fun refreshSnapshot(
        medicationId: Long,
        scheduledAt: Instant,
        name: String,
        amountValue: BigDecimal,
        amountUnit: String,
    )

    @Query("UPDATE doses SET outcome = :outcome, recorded_at = :at, snoozed_until = NULL WHERE id = :id")
    suspend fun setIntake(id: Long, outcome: String, at: Instant)

    /** A snooze is an acknowledgement, so it also puts the repeat sequence back to the start. */
    @Query("UPDATE doses SET snoozed_until = :until, reminder_count = 0 WHERE id = :id")
    suspend fun setSnooze(id: Long, until: Instant?)

    @Query("UPDATE doses SET first_reminded_at = :at WHERE id = :id")
    suspend fun setFirstReminded(id: Long, at: Instant)

    @Query("UPDATE doses SET reminder_count = reminder_count + 1 WHERE id = :id")
    suspend fun incrementReminderCount(id: Long)

    @Query(
        """
        SELECT MIN(scheduled_at) FROM doses
        WHERE medication_id = :medicationId AND scheduled_at > :after
        """,
    )
    suspend fun nextScheduledAtAfter(medicationId: Long, after: Instant): Instant?

    /**
     * One medicine's record over a window, answered and unanswered doses alike, served by the
     * `(medication_id, scheduled_at)` index the planner already needs (app-medicine-usage-history
     * design D1). Nothing here writes.
     */
    @Query(
        """
        SELECT * FROM doses
        WHERE medication_id = :medicationId AND scheduled_at >= :from AND scheduled_at < :to
        ORDER BY scheduled_at ASC
        """,
    )
    fun observeHistoryFor(medicationId: Long, from: Instant, to: Instant): Flow<List<DoseEntity>>

    /** The oldest moment this medicine has a stored dose for, or null when it has none. */
    @Query("SELECT MIN(scheduled_at) FROM doses WHERE medication_id = :medicationId")
    suspend fun earliestScheduledAt(medicationId: Long): Instant?

    /** Only for tests and for the debug preview data; production never removes a dose. */
    @Query("DELETE FROM doses")
    suspend fun deleteAll()
}
