package nl.hexmaster.pillsner.domain.scheduling

import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.PlannedDose
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.repository.MedicationRepository

/**
 * What a refresh did: the doses it withdrew, and the medication list it read to do it — handed
 * back so a caller elsewhere in the same wake (namely [ComputeWakeSchedule]) does not have to read
 * the same list again (reminder-wake-cycle-db-efficiency design D2).
 */
data class RefreshResult(val withdrawn: List<DoseId>, val medications: List<Medication>)

/**
 * Keeps the stored doses for today and tomorrow equal to what the active medicines' schedules say
 * (design D3).
 *
 * The window is deliberately short: it is what the Home screen shows, it is enough for the missed
 * rule, and a small table means an edit reconciles quickly. Running this twice in a row changes
 * nothing, which is what lets it run on every wake, on every medication change and at the day
 * rollover without any bookkeeping.
 *
 * Three things happen, in order, and each is idempotent:
 *
 * 1. **Add** the planned doses that are not stored yet.
 * 2. **Refresh** the name and amount of the pending ones. Those columns exist so a dose survives
 *    its medicine, but while a dose is pending they must follow the medicine: a Home tile or a
 *    reminder that names a medicine the user has just renamed is simply wrong. A dose the user has
 *    answered keeps what it was recorded with, for good.
 * 3. **Withdraw** the pending doses the schedules no longer call for, matching each dose against
 *    its *own* medicine's moments.
 *
 * A dose the user has answered is never touched. Those are facts; only doses that are still merely
 * planned may be withdrawn.
 *
 * A dose the user has already been reminded about is withdrawn only when [afterUserEdit] is set.
 * The two callers want opposite things and the refresh cannot tell them apart on its own: when the
 * user has just changed a medicine they have said they no longer take it then, so an outstanding
 * reminder for it must go; when the clock or the time zone moved, `reminder-scheduling` requires a
 * dose the user has already been told about to keep its moment and not be reminded again.
 */
class RefreshPlannedDoses(
    private val medicationRepository: MedicationRepository,
    private val doseRepository: DoseRepository,
    private val generator: DoseGenerator = DoseGenerator(),
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    /**
     * @param afterUserEdit true only when this refresh follows a change the user made to a medicine
     *   or its schedules, which is the one case that may withdraw an already-reminded dose.
     * @return the doses this refresh withdrew, so the caller can take down anything it has shown
     *   the user for them, and the medication list this refresh read.
     */
    suspend operator fun invoke(afterUserEdit: Boolean = false): RefreshResult {
        val zone = clock.zone
        val today = LocalDate.now(clock)
        val window = today..today.plusDays(WINDOW_DAYS)

        val medications = medicationRepository.observeAll().first()

        // Every known medicine appears, including the ones that plan nothing at all — inactive,
        // without schedules, or outside the days they are used. Their empty entry is what withdraws
        // the doses they left behind.
        val plannedByMedication: Map<MedicationId, List<PlannedDose>> =
            medications.associate { it.id to generator.plan(it, window, zone) }

        val planned: List<PlannedDose> = plannedByMedication.values.flatten()

        // The moment of storing, kept for good on each new row: it is what later tells a reminder
        // the platform dropped from a dose generated after its own moment, which never had one to
        // drop (design D4). A dose already stored keeps the moment it first appeared.
        doseRepository.insertPlanned(planned, clock.instant())
        doseRepository.refreshSnapshots(planned)

        val moments = plannedByMedication.mapValues { (_, doses) -> doses.map { it.scheduledAt } }

        // A day of slack on each side of the window. Moving time zone shifts every wall-clock
        // moment by up to a day, so a dose planned in the old zone can land just outside the new
        // window; without the slack it would survive as a stale reminder. Doses the user has
        // already answered are protected by the repository regardless.
        val withdrawn = doseRepository.withdrawPlanned(
            from = zone.startOfDay(window.start.minusDays(SLACK_DAYS)),
            to = zone.startOfDay(window.endInclusive.plusDays(1 + SLACK_DAYS)),
            planned = moments,
            includeReminded = false,
        )

        if (!afterUserEdit) return RefreshResult(withdrawn, medications)

        // The user has just changed a medicine, so a reminder still showing for a dose they no
        // longer take is wrong and goes too. Only within the window the plan actually covers: a
        // dose out in the slack is not planned simply because the window does not reach it, and
        // withdrawing an unanswered reminder on that basis would lose something the user still
        // owes an answer to.
        val alsoWithdrawn = doseRepository.withdrawPlanned(
            from = zone.startOfDay(window.start),
            to = zone.startOfDay(window.endInclusive.plusDays(1)),
            planned = moments,
            includeReminded = true,
        )
        return RefreshResult(withdrawn + alsoWithdrawn, medications)
    }

    private fun ZoneId.startOfDay(date: LocalDate) =
        ZonedDateTime.of(date, LocalTime.MIN, this).toInstant()

    private companion object {
        /** Today plus this many days. Two days in total: today and tomorrow. */
        const val WINDOW_DAYS = 1L

        /** Enough to absorb the largest time zone shift there is. */
        const val SLACK_DAYS = 1L
    }
}
