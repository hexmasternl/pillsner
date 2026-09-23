package nl.hexmaster.pillsner.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate

/** One row of the `medications` table (design D3). */
@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "default_dose_value") val defaultDoseValue: BigDecimal,
    @ColumnInfo(name = "default_dose_unit") val defaultDoseUnit: String,
    @ColumnInfo(name = "used_since") val usedSince: LocalDate,
    @ColumnInfo(name = "use_until") val useUntil: LocalDate?,
    @ColumnInfo(name = "prescribed_by") val prescribedBy: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    /** Null unless the user has said they ordered more while stock was low (`medicine-stock-tracking`). */
    @ColumnInfo(name = "low_stock_acknowledgement") val lowStockAcknowledgement: String? = null,
)
