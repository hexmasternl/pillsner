package nl.hexmaster.pillsner.domain.repository

import kotlinx.coroutines.flow.Flow
import nl.hexmaster.pillsner.domain.model.UpcomingDose

/** Source of the doses the user still has ahead of them. */
interface UpcomingDosesRepository {

    /**
     * Observes the upcoming doses.
     *
     * Contract: the emitted list is sorted by [UpcomingDose.scheduledAt] ascending (soonest first)
     * and contains at most [limit] items. Doses whose scheduled moment has passed are not included;
     * what happens to those is defined by intake tracking. A new list is emitted whenever the set
     * of upcoming doses changes, so a collector can keep a screen current without polling.
     *
     * @param limit maximum number of doses to emit; must be at least 1.
     */
    fun observeUpcoming(limit: Int): Flow<List<UpcomingDose>>
}
