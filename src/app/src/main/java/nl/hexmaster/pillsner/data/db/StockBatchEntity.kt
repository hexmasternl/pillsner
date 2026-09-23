package nl.hexmaster.pillsner.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * One row of the `stock_batches` table (`medicine-stock-tracking`).
 *
 * Cascades on medication delete for schema consistency with `schedules`, though in practice a
 * medication row is never deleted outside the full app reset (`medication-persistence`'s "Medications
 * are never removed" requirement).
 */
@Entity(
    tableName = "stock_batches",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["medication_id", "expiry_date"])],
)
data class StockBatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "medication_id") val medicationId: Long,
    @ColumnInfo(name = "remaining_value") val remainingValue: BigDecimal,
    @ColumnInfo(name = "remaining_unit") val remainingUnit: String,
    /** How much of the medication's default dose unit one unit of this batch is worth; 1 when they match. */
    @ColumnInfo(name = "strength_per_unit") val strengthPerUnit: BigDecimal,
    @ColumnInfo(name = "expiry_date") val expiryDate: LocalDate,
    @ColumnInfo(name = "added_at") val addedAt: Instant,
)
