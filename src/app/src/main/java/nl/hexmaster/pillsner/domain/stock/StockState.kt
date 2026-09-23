package nl.hexmaster.pillsner.domain.stock

import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import nl.hexmaster.pillsner.domain.model.BatchExpiryState
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.StockBatch

/** Default lead time the expiry checks warn within (`medicine-stock-tracking`). */
const val APPROACHING_EXPIRY_WINDOW_DAYS = 30L

/**
 * How adequate a medicine's remaining stock is, for the tile's always-on stock indicator
 * (`medicine-stock-tracking`'s "Medicine tile stock level indicator" requirement). An as-needed
 * medicine, which has no weekly projection to compare against, is always [SUFFICIENT].
 */
enum class StockLevel {
    /** Enough remains to cover the projected week. */
    SUFFICIENT,

    /** Less than a projected week remains, but some stock is left. */
    LOW,

    /** Nothing usable remains — the one case the design system reserves red for besides danger. */
    CRITICAL,
}

/**
 * A medicine's current stock picture, read live rather than tied to any one taken dose: how
 * adequate its stock is, and the expiry state of its soonest-expiring batch that still has stock.
 *
 * Used identically by the tile heads-up, the Medicine details inline note, and stock-warning
 * evaluation, so all three always agree (`medicine-stock-tracking`'s "Tile and details-screen
 * heads-up is a live read, not an event" decision).
 */
data class StockState(val level: StockLevel, val nearestExpiry: BatchExpiryState) {
    /** True whenever [level] is not [StockLevel.SUFFICIENT] — the pre-existing low-stock warning. */
    val isLow: Boolean get() = level != StockLevel.SUFFICIENT
}

/**
 * The current stock picture for [medication] given its [batches]. A medicine with no batches at all
 * is exempt from this feature entirely; callers guard on that first
 * (`medicine-stock-tracking`'s "A medicine with no stock recorded is unaffected" requirement).
 */
fun stockState(
    batches: List<StockBatch>,
    medication: Medication,
    today: LocalDate,
    zone: ZoneId,
    projectWeeklyUsage: ProjectWeeklyUsage = ProjectWeeklyUsage(),
): StockState {
    // Every batch's remaining amount, converted to the medicine's default dose unit via its own
    // strength, so a mix of units across batches still totals correctly (`medicine-stock-tracking`'s
    // "Stock unit conversion" requirement).
    val remainingInDoseUnits = batches.fold(BigDecimal.ZERO) { sum, batch -> sum + batch.remainingInDoseUnits }
    // Projected through these same batches, so whole pills used by the rounding count towards the
    // week (`medicine-stock-tracking`'s "Weekly usage projection" requirement).
    val weeklyUsage = projectWeeklyUsage.forMedication(medication, today, zone, batches)
    val level = when {
        weeklyUsage == null -> StockLevel.SUFFICIENT
        remainingInDoseUnits <= BigDecimal.ZERO -> StockLevel.CRITICAL
        remainingInDoseUnits < weeklyUsage.value -> StockLevel.LOW
        else -> StockLevel.SUFFICIENT
    }

    val nearestExpiry = batches
        .filter { it.usableRemaining > BigDecimal.ZERO }
        .minByOrNull { it.expiryDate }
        ?.let { batchExpiryState(it.expiryDate, today) }
        ?: BatchExpiryState.NONE

    return StockState(level, nearestExpiry)
}

/**
 * Classifies [expiryDate] against [today]: past when it has already gone by, approaching within
 * [approachingWindowDays] of it (today counts as within the window), otherwise none.
 */
fun batchExpiryState(
    expiryDate: LocalDate,
    today: LocalDate,
    approachingWindowDays: Long = APPROACHING_EXPIRY_WINDOW_DAYS,
): BatchExpiryState = when {
    expiryDate.isBefore(today) -> BatchExpiryState.PAST
    ChronoUnit.DAYS.between(today, expiryDate) <= approachingWindowDays -> BatchExpiryState.APPROACHING
    else -> BatchExpiryState.NONE
}
