package nl.hexmaster.pillsner.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant

/**
 * One row of the `doses` table (design D9).
 *
 * The medication reference is set to null rather than cascaded when a medication row goes, because
 * a dose is the user's record of what they took and must outlive the medicine. The name and amount
 * snapshots are what keep such a row readable on its own.
 */
@Entity(
    tableName = "doses",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["medication_id", "scheduled_at"], unique = true),
        Index(value = ["scheduled_at"]),
    ],
)
data class DoseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "medication_id") val medicationId: Long?,
    @ColumnInfo(name = "medication_name") val medicationName: String,
    @ColumnInfo(name = "amount_value") val amountValue: BigDecimal,
    @ColumnInfo(name = "amount_unit") val amountUnit: String,
    @ColumnInfo(name = "scheduled_at") val scheduledAt: Instant,
    /** TAKEN, SKIPPED or MISSED; null while the dose is pending. */
    val outcome: String? = null,
    @ColumnInfo(name = "recorded_at") val recordedAt: Instant? = null,
    @ColumnInfo(name = "snoozed_until") val snoozedUntil: Instant? = null,
    @ColumnInfo(name = "first_reminded_at") val firstRemindedAt: Instant? = null,
)
