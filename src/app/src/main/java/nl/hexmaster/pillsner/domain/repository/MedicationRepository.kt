package nl.hexmaster.pillsner.domain.repository

import kotlinx.coroutines.flow.Flow
import nl.hexmaster.pillsner.domain.model.LowStockAcknowledgement
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication

/**
 * Source of the medications the user has entered, and the way they are stored and changed.
 *
 * There is deliberately **no removal operation**. A medicine can never be removed from Pillsner: a
 * user who stops taking something deactivates it, and the record, its schedules and every dose it
 * ever produced stay on the device. Adding a delete here would throw away the user's own history.
 */
interface MedicationRepository {

    /**
     * Observes every medication, active and inactive, with its schedules.
     *
     * Contract: the emitted list holds all medications in no guaranteed order. Ordering by name is
     * locale-sensitive presentation and splitting active from inactive is a view concern, so both
     * stay in the view model. A new list is emitted whenever any medication or schedule is added or
     * changed, so a collector keeps a screen current without polling.
     */
    fun observeAll(): Flow<List<Medication>>

    /** One medication with its schedules, or null when no medication has [id]. */
    suspend fun get(id: MedicationId): Medication?

    /**
     * Stores [medication] and its schedules as one active medication, in a single transaction, and
     * returns the identifier it was given. A medication is never stored without its schedules.
     */
    suspend fun add(medication: NewMedication): MedicationId

    /**
     * Replaces every field and every schedule of the medication that has [medication]'s identifier,
     * in one transaction, and re-emits to observers. Dose rows are untouched: what the user has
     * already taken is history, and what is still only planned is re-planned by the reminder layer
     * on the next emission.
     *
     * @throws IllegalStateException when no medication has that identifier. Unlike [setActive] this
     *   never fails quietly: it carries a form the user has spent time on.
     */
    suspend fun update(medication: Medication)

    /**
     * Sets whether the medication with [id] currently produces doses.
     *
     * A medication is never removed, only stopped: its record, its schedules and its dose history
     * stay on the device. Observers re-emit, which is what moves the tile to the other section and
     * what makes the reminder coordinator drop or re-plan its doses. An unknown [id] does nothing:
     * the stream will correct the screen anyway.
     */
    suspend fun setActive(id: MedicationId, isActive: Boolean)

    /**
     * Sets, or with null clears, the low-stock acknowledgement of the medication with [id]
     * (`medicine-stock-tracking`). Choosing "OK" on the low-stock warning leaves this unset, so the
     * warning returns on the next taken dose that still leaves stock low; choosing "I ordered new"
     * sets it, and adding a new stock batch always clears it again. An unknown [id] does nothing.
     */
    suspend fun setLowStockAcknowledgement(id: MedicationId, value: LowStockAcknowledgement?)
}
