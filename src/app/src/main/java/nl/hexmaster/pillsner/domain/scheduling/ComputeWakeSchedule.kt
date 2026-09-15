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
 * Every moment the app has to wake up for (design D2).
 *
 * This replaces the single next-wake moment the app used to keep. One alarm for the earliest thing
 * made the schedule a chain — the only place the next alarm was armed was inside the wake the
 * current one triggered — so the platform deferring or dropping one alarm stopped every reminder
 * after it. That happened in the field. One alarm per moment makes losing an alarm cost one
 * reminder instead of all of them.
 *
 * The moments are: every dose still to be announced, every snooze that runs out, every reminder due
 * to ask again, and one housekeeping moment — the earliest of the next dose lapse and the daily
 * refresh just after midnight that rolls the planning window forward. The window is two days and a
 * medicine is due a handful of times a day, so the set stays in single digits.
 */
class ComputeWakeSchedule(
    private val doseRepository: DoseRepository,
    private val medicationRepository: MedicationRepository,
    private val markMissedDoses: MarkMissedDoses,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    /** @return every moment to wake at, or the empty set when there is nothing to wake up for. */
    suspend operator fun invoke(): WakeSchedule {
        val now = clock.instant()
        val moments = mutableSetOf<WakeMoment>()
        val housekeeping = mutableListOf<Instant>()

        doseRepository.pending().forEach { dose ->
            val lapseAt = markMissedDoses.lapseAt(dose)

            if (dose.firstRemindedAt == null && dose.scheduledAt.isAfter(now)) {
                moments += WakeMoment(dose.scheduledAt, WakeKind.REMINDER)
            }
            dose.snoozedUntil?.takeIf { it.isAfter(now) }?.let {
                moments += WakeMoment(it, WakeKind.REMINDER)
            }
            ReminderRepeats.nextRepeatAt(dose, lapseAt)?.takeIf { it.isAfter(now) }?.let {
                moments += WakeMoment(it, WakeKind.REMINDER)
            }

            lapseAt.takeIf { it.isAfter(now) }?.let { housekeeping += it }
        }

        if (anyMedicineProducesDoses()) housekeeping += nextDailyRefresh(now)

        // One housekeeping alarm rather than one per dose: nothing the user sees depends on it
        // firing to the minute, and the wake it triggers settles everything that has lapsed at
        // once, then works out the next one.
        housekeeping.minOrNull()?.let { moments += WakeMoment(it, WakeKind.HOUSEKEEPING) }

        return moments
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
