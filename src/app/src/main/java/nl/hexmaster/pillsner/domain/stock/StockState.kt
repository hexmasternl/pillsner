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
 * A medicine's current stock picture, read live rather than tied to any one taken dose: whether it
 * is running low, and the expiry state of its soonest-expiring batch that still has stock.
 *
 * Used identically by the tile heads-up, the Medicine details inline note, and stock-warning
 * evaluation, so all three always agree (`medicine-stock-tracking`'s "Tile and details-screen
 * heads-up is a live read, not an event" decision).
 */
data class StockState(val isLow: Boolean, val nearestExpiry: BatchExpiryState)

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
    val weeklyUsage = projectWeeklyUsage.forMedication(medication, today, zone)
    val isLow = weeklyUsage != null && remainingInDoseUnits < weeklyUsage.value

    val nearestExpiry = batches
        .filter { it.remaining > BigDecimal.ZERO }
        .minByOrNull { it.expiryDate }
        ?.let { batchExpiryState(it.expiryDate, today) }
        ?: BatchExpiryState.NONE

    return StockState(isLow, nearestExpiry)
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
