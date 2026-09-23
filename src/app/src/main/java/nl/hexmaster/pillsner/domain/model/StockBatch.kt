package nl.hexmaster.pillsner.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate

/** Identifies one stock batch. Stable for the life of the batch; Room assigns it. */
@JvmInline
value class StockBatchId(val value: Long)

/**
 * One batch of a medication's physical stock: an amount remaining, in its own unit, and the expiry
 * date that batch of packaging carries (`medicine-stock-tracking`).
 *
 * A medication's stock is the set of its batches; a medication with none is left untouched by the
 * feature entirely. [unit] MAY differ from the medication's own default dose unit — a medicine dosed
 * in milligrams can still have its stock counted in tablets — in which case [strengthPerUnit] is how
 * much of the medication's default dose unit one unit of this batch is worth (for example, 20 for a
 * batch of 20 mg tablets). When [unit] matches the medication's default dose unit, [strengthPerUnit]
 * is exactly 1: a batch never needs converting against itself. Every quantity the stock system
 * compares or totals across batches does so in the medication's default dose unit, by multiplying a
 * batch's [remaining] by its own [strengthPerUnit] (`medicine-stock-tracking`'s "Stock unit
 * conversion" requirement); [remaining] and [unit] alone are for display.
 *
 * A batch consumed down to zero is kept, not removed, so a medicine that has ever had stock recorded
 * never silently reverts to having none, which would otherwise stop it warning at the exact moment
 * stock runs out.
 *
 * @property addedAt when this batch was added, used only to break a tie between two batches that
 *   share the same [expiryDate] when consuming first-expiry-first-out.
 */
data class StockBatch(
    val id: StockBatchId,
    val medicationId: MedicationId,
    val remaining: BigDecimal,
    val unit: DoseUnit,
    val strengthPerUnit: BigDecimal,
    val expiryDate: LocalDate,
    val addedAt: Instant,
) {
    init {
        require(remaining >= BigDecimal.ZERO) { "A stock batch's remaining amount cannot be negative" }
        require(strengthPerUnit > BigDecimal.ZERO) { "A stock batch's strength must be greater than zero" }
    }

    /**
     * What this batch can actually still give, in its own [unit]: [remaining] rounded down to a
     * whole number when [unit] is a whole-pill unit, otherwise [remaining] itself. A tablet batch
     * can only hold a fraction if it was recorded before whole-pill deduction existed (49.92
     * tablets, say); that fraction is a pill already used, so it reads as 49 everywhere and is
     * written back as a whole number the next time a dose is drawn from this batch
     * (`medicine-stock-tracking`'s "Whole-pill units" requirement).
     */
    val usableRemaining: BigDecimal
        get() = if (unit.isWholePill) remaining.setScale(0, RoundingMode.FLOOR) else remaining

    /** [usableRemaining], converted to the medication's default dose unit via [strengthPerUnit]. */
    val remainingInDoseUnits: BigDecimal get() = usableRemaining * strengthPerUnit
}

/**
 * How close a stock batch is to, or past, its expiry date, evaluated against "today"
 * (`medicine-stock-tracking`).
 */
enum class BatchExpiryState { NONE, APPROACHING, PAST }

/**
 * A ready-to-show stock warning for one medicine: at least one of [lowStock] or [expiryState] must
 * be the thing worth saying, per `medicine-stock-tracking`'s "Low-stock warning" and "Expiry-at-use
 * warning" requirements.
 *
 * @property lowStock whether the medicine's remaining stock does not cover its projected weekly
 *   usage and the user has not suppressed the warning with "I ordered new".
 * @property expiryState the soonest-expiring batch with remaining stock's state; `NONE` means
 *   nothing to say about expiry, independent of [lowStock].
 */
data class StockWarning(
    val medicationId: MedicationId,
    val medicationName: String,
    val lowStock: Boolean,
    val expiryState: BatchExpiryState,
) {
    init {
        require(lowStock || expiryState != BatchExpiryState.NONE) {
            "A stock warning must have something to warn about"
        }
    }
}
