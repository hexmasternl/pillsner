package nl.hexmaster.pillsner.domain.scheduling

import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first
import nl.hexmaster.pillsner.domain.model.PlannedDose
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.repository.MedicationRepository

/**
 * Keeps the stored doses for today and tomorrow equal to what the active medicines' schedules say
 * (design D3).
 *
 * The window is deliberately short: it is what the Home screen shows, it is enough for the missed
 * rule, and a small table means an edit reconciles quickly. Running this twice in a row changes
 * nothing, which is what lets it run on every wake, on every medication change and at the day
 * rollover without any bookkeeping.
 *
 * A dose the user has answered, or has already been reminded about, is never touched. Those are
 * facts; only doses that are still merely planned may be withdrawn.
 */
class RefreshPlannedDoses(
    private val medicationRepository: MedicationRepository,
    private val doseRepository: DoseRepository,
    private val generator: DoseGenerator = DoseGenerator(),
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    suspend operator fun invoke() {
        val zone = clock.zone
        val today = LocalDate.now(clock)
        val window = today..today.plusDays(WINDOW_DAYS)

        val planned: List<PlannedDose> = medicationRepository.observeAll().first()
            .flatMap { generator.plan(it, window, zone) }

        doseRepository.insertPlanned(planned)

        // A day of slack on each side of the window. Moving time zone shifts every wall-clock
        // moment by up to a day, so a dose planned in the old zone can land just outside the new
        // window; without the slack it would survive as a stale reminder. Doses the user has
        // already been told about, or answered, are protected by the repository regardless.
        doseRepository.deletePlannedNotIn(
            from = ZonedDateTime.of(window.start.minusDays(SLACK_DAYS), LocalTime.MIN, zone).toInstant(),
            to = ZonedDateTime.of(
                window.endInclusive.plusDays(1 + SLACK_DAYS),
                LocalTime.MIN,
                zone,
            ).toInstant(),
            keep = planned.map { it.scheduledAt },
        )
    }

    private companion object {
        /** Today plus this many days. Two days in total: today and tomorrow. */
        const val WINDOW_DAYS = 1L

        /** Enough to absorb the largest time zone shift there is. */
        const val SLACK_DAYS = 1L
    }
}
