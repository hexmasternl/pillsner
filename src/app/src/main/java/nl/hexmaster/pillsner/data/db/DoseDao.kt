package nl.hexmaster.pillsner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
     * Drops the planned doses in a window that the schedules no longer call for. A dose with an
     * outcome, or one the user has already been reminded about, is never removed.
     */
    @Query(
        """
        DELETE FROM doses
        WHERE outcome IS NULL
          AND first_reminded_at IS NULL
          AND scheduled_at >= :from AND scheduled_at < :to
          AND scheduled_at NOT IN (:keep)
        """,
    )
    suspend fun deletePlannedNotIn(from: Instant, to: Instant, keep: Collection<Instant>)

    @Query("UPDATE doses SET outcome = :outcome, recorded_at = :at, snoozed_until = NULL WHERE id = :id")
    suspend fun setIntake(id: Long, outcome: String, at: Instant)

    @Query("UPDATE doses SET snoozed_until = :until WHERE id = :id")
    suspend fun setSnooze(id: Long, until: Instant?)

    @Query("UPDATE doses SET first_reminded_at = :at WHERE id = :id")
    suspend fun setFirstReminded(id: Long, at: Instant)

    @Query(
        """
        SELECT MIN(scheduled_at) FROM doses
        WHERE medication_id = :medicationId AND scheduled_at > :after
        """,
    )
    suspend fun nextScheduledAtAfter(medicationId: Long, after: Instant): Instant?

    /** Only for tests and for the debug preview data; production never removes a dose. */
    @Query("DELETE FROM doses")
    suspend fun deleteAll()
}
