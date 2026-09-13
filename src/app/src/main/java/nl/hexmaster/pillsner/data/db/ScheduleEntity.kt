package nl.hexmaster.pillsner.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalTime

/** Which shape a [ScheduleEntity] row holds; decides which nullable columns must be present. */
object ScheduleKind {
    const val EVERY_N_DAYS = "EVERY_N_DAYS"
    const val ON_WEEKDAYS = "ON_WEEKDAYS"
    const val EVERY_N_HOURS = "EVERY_N_HOURS"
}

/**
 * One row of the `schedules` table (design D3).
 *
 * One flat row per schedule with nullable columns per shape, rather than a table per shape: it
 * keeps reading a medication to a single relation and the sealed-interface mapping to one `when`.
 * The nullable columns are safe because the mapper refuses to build a half-valid schedule and
 * throws instead.
 */
@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("medication_id")],
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "medication_id") val medicationId: Long,
    /** The schedule's place in the medication's list, so order survives a round trip. */
    val position: Int,
    val kind: String,
    @ColumnInfo(name = "amount_value") val amountValue: BigDecimal,
    @ColumnInfo(name = "amount_unit") val amountUnit: String,
    @ColumnInfo(name = "interval_days") val intervalDays: Int? = null,
    @ColumnInfo(name = "interval_hours") val intervalHours: Int? = null,
    val days: Set<DayOfWeek>? = null,
    val times: List<LocalTime>? = null,
    @ColumnInfo(name = "first_dose_at") val firstDoseAt: LocalTime? = null,
)
