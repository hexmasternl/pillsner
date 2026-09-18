package nl.hexmaster.pillsner.domain.repository

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.PlannedDose

/**
 * One dose's reminder posting to apply through [DoseRepository.applyReminderOutcomes]: the moment
 * it was posted, whether the posting counts as a repeat, and whether it also clears an outstanding
 * snooze — the same information a [DoseRepository.recordReminded] call followed, when needed, by a
 * [DoseRepository.setSnooze] call to null would carry, batched for one transaction instead of two
 * suspend calls per dose (reminder-wake-cycle-db-efficiency design D3).
 */
data class ReminderOutcomeUpdate(
    val id: DoseId,
    val at: Instant,
    val countsAsRepeat: Boolean,
    val clearsSnooze: Boolean,
)

/** Storage of the doses the app has planned and of what became of them. */
interface DoseRepository {

    /** Every dose the user has not answered yet, ordered by scheduled moment, re-emitting on change. */
    fun observePending(): Flow<List<Dose>>

    /**
     * The pending doses from [from] onwards, soonest first, at most [limit] of them. Doses whose
     * moment has passed but that are still unanswered are included: they are still to be taken.
     */
    fun observeUpcoming(from: Instant, limit: Int): Flow<List<Dose>>

    /** Every pending dose, as one snapshot rather than a stream, for the reminder coordinator. */
    suspend fun pending(): List<Dose>

    /** Loads one dose, or null when it is not there. */
    suspend fun get(id: DoseId): Dose?

    /**
     * One dose as a stream: re-emits when it is answered, snoozed or withdrawn.
     *
     * A null emission means the dose is gone — withdrawn by a refresh because the user changed the
     * medicine it came from. A screen showing one dose needs this rather than [get], because the
     * dose can be answered from the notification shade or from a watch while that screen is in the
     * foreground, and what it shows must follow.
     */
    fun observe(id: DoseId): Flow<Dose?>

    /**
     * Stores every dose of [doses] that is not stored yet. A dose already present for the same
     * medication and moment is left exactly as it is, which is what makes refreshing idempotent.
     *
     * @param plannedAt the moment these new rows are being stored, which each one keeps for good.
     *   A dose that is already there keeps the moment it first appeared, so the column never moves
     *   under a refresh (design D4).
     */
    suspend fun insertPlanned(doses: List<PlannedDose>, plannedAt: Instant)

    /**
     * Brings the name and amount of the pending doses matching [doses] up to date with the medicine
     * each one belongs to.
     *
     * A dose keeps a copy of its medicine's name and amount so that it stays readable once the
     * medicine is gone, but while the dose is still pending that copy must follow the medicine:
     * a screen or a reminder naming a medicine the user has just renamed is wrong. A dose that has
     * an outcome is the user's record of what happened and is never changed.
     */
    suspend fun refreshSnapshots(doses: List<PlannedDose>)

    /**
     * Withdraws the pending doses scheduled within `[from, to)` that the medicines' schedules no
     * longer call for, and returns the ones it withdrew.
     *
     * [planned] holds every known medicine, mapped to the moments it now plans inside the window;
     * a medicine that plans nothing — deactivated, without schedules, or outside the days it is
     * used — maps to an empty list and loses all of its pending doses in the window. A dose is
     * matched against **its own** medicine's moments, so another medicine planning a dose at the
     * same instant never keeps it alive.
     *
     * [includeReminded] says whether a dose the user has already been reminded about may go. It is
     * true only when this follows a change the user made to a medicine: they have just said they no
     * longer take it then, so an outstanding reminder for it is wrong. It is false for a clock
     * change, a time-zone change, a reboot or an ordinary wake, where a dose the user has already
     * been told about keeps its moment.
     *
     * A dose with an outcome is never withdrawn, under either mode.
     */
    suspend fun withdrawPlanned(
        from: Instant,
        to: Instant,
        planned: Map<MedicationId, List<Instant>>,
        includeReminded: Boolean,
    ): List<DoseId>

    /** Records the outcome of one dose and clears any snooze on it. */
    suspend fun recordIntake(id: DoseId, outcome: IntakeOutcome, at: Instant)

    /**
     * Sets, or with null clears, when a pending dose should be reminded about again.
     *
     * A snooze also resets the repeat count: the user has acknowledged the dose, so the repeats
     * they collected before saying "Not yet" must not count against them afterwards.
     */
    suspend fun setSnooze(id: DoseId, until: Instant?)

    /**
     * Records that the reminder for this dose has just been posted, at [at].
     *
     * The first posting is also the moment the user was first told, which is a fact about the dose
     * and is never rewritten afterwards. Every posting moves the anchor the repeat rule counts
     * from, so the next ask is a quarter of an hour after the last one rather than after the first.
     *
     * @param countsAsRepeat true when this posting was the repeat rule asking again. A dose falling
     *   due and a snooze running out are not repeats: the first starts the sequence and the second
     *   restarts it, because the user has acknowledged the dose.
     *
     * Kept in storage rather than in memory because a repeat sequence outlives the process: the app
     * is asleep between one ask and the next, and may well be started fresh by the alarm.
     */
    suspend fun recordReminded(id: DoseId, at: Instant, countsAsRepeat: Boolean)

    /**
     * Applies every [ReminderOutcomeUpdate] of [updates] in one transaction: for each one, records
     * its reminder as posted, then — only where the update says so — clears its snooze. This is the
     * batched form of calling [recordReminded] and, conditionally, [setSnooze] once per dose in a
     * loop, for the wake cycle's due-dose posting step.
     */
    suspend fun applyReminderOutcomes(updates: List<ReminderOutcomeUpdate>)

    /**
     * The moment of the next dose of [medicationId] after [after], or null when there is none in
     * the stored window. Used to decide when an unanswered dose is superseded.
     */
    suspend fun nextScheduledAtAfter(medicationId: MedicationId, after: Instant): Instant?

    /**
     * Every stored dose of [medicationId] scheduled in `[from, to)`, oldest first, answered and
     * unanswered alike. Read-only: nothing here changes a dose, and history is never withdrawn, so
     * what comes back is what happened. Re-emits when an outcome is recorded while it is collected.
     */
    fun observeHistoryFor(medicationId: MedicationId, from: Instant, to: Instant): Flow<List<Dose>>

    /**
     * The moment of [medicationId]'s oldest stored dose, or null when it has none. Read-only, and
     * the honest answer to "how far back do the records reach": an empty stretch inside a window is
     * a medicine taken rarely, not a record that is missing.
     */
    suspend fun earliestScheduledAt(medicationId: MedicationId): Instant?

    /**
     * Deletes every dose row scheduled before [cutoff] — taken, skipped and missed alike — and
     * returns how many rows were removed (dose-history-retention design D4).
     *
     * A pending dose is never old enough to qualify in practice: the rolling planning window
     * never lets one reach anywhere near a year old. `Medication` and `Schedule` rows are never
     * touched by this call.
     */
    suspend fun deleteHistoryBefore(cutoff: Instant): Int
}
