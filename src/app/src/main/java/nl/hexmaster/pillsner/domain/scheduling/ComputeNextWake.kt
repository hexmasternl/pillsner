package nl.hexmaster.pillsner.domain.scheduling

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.repository.MedicationRepository

/**
 * The single moment the app next has to wake up (design D5).
 *
 * One alarm, not one per dose: whatever changed, the answer is the earliest of everything that
 * still has to happen. That keeps the scheduling trivially correct and stays well inside the
 * platform's limits on waking an idle device.
 *
 * The candidates are the first dose that still has to be announced, the first snooze that runs
 * out, the first dose that lapses, and the daily refresh just after midnight that rolls the
 * planning window forward.
 */
class ComputeNextWake(
    private val doseRepository: DoseRepository,
    private val medicationRepository: MedicationRepository,
    private val markMissedDoses: MarkMissedDoses,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    /** @return the next moment to wake, or null when there is nothing at all to wake up for. */
    suspend operator fun invoke(): Instant? {
        val now = clock.instant()
        val candidates = mutableListOf<Instant>()

        doseRepository.pending().forEach { dose ->
            if (dose.firstRemindedAt == null && dose.scheduledAt.isAfter(now)) {
                candidates += dose.scheduledAt
            }
            dose.snoozedUntil?.takeIf { it.isAfter(now) }?.let { candidates += it }
            markMissedDoses.lapseAt(dose).takeIf { it.isAfter(now) }?.let { candidates += it }
        }

        if (anyMedicineProducesDoses()) candidates += nextDailyRefresh(now)

        return candidates.minOrNull()
    }

    /** Without an active, scheduled medicine there is nothing for the daily refresh to plan. */
    private suspend fun anyMedicineProducesDoses(): Boolean =
        medicationRepository.observeAll().first().any { it.isActive && it.schedules.isNotEmpty() }

    /**
     * Just after midnight rather than at it: a few minutes of slack keeps the refresh clear of the
     * moment the date itself changes, and no dose is ever planned for 00:05 by accident.
     */
    private fun nextDailyRefresh(now: Instant): Instant {
        val zone = clock.zone
        val today = LocalDate.now(clock)
        val todayRefresh = ZonedDateTime.of(today, DAILY_REFRESH_TIME, zone).toInstant()
        return if (todayRefresh.isAfter(now)) {
            todayRefresh
        } else {
            ZonedDateTime.of(today.plusDays(1), DAILY_REFRESH_TIME, zone).toInstant()
        }
    }

    companion object {
        /** Local time at which the planning window rolls forward a day. */
        val DAILY_REFRESH_TIME: LocalTime = LocalTime.of(0, 5)
    }
}
