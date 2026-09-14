package nl.hexmaster.pillsner.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import nl.hexmaster.pillsner.domain.model.UpcomingDose
import nl.hexmaster.pillsner.domain.repository.UpcomingDosesRepository

/**
 * Placeholder until medications and schedules exist: there are never any upcoming doses,
 * so the welcome screen shows its empty state on a real device (design D5).
 */
class EmptyUpcomingDosesRepository : UpcomingDosesRepository {
    override fun observeUpcoming(limit: Int): Flow<List<UpcomingDose>> = flowOf(emptyList())
}
