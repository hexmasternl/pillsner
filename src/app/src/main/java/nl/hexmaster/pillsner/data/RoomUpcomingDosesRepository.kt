package nl.hexmaster.pillsner.data

import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.UpcomingDose
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.repository.UpcomingDosesRepository

/**
 * The welcome screen's view of the dose table: what the user still has to take, soonest first.
 *
 * A dose whose moment has passed but that has not been answered stays in the list and is marked
 * overdue, because "what do I still have to take" includes exactly that dose.
 */
class RoomUpcomingDosesRepository(
    private val doseRepository: DoseRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : UpcomingDosesRepository {

    override fun observeUpcoming(limit: Int): Flow<List<UpcomingDose>> =
        doseRepository.observeUpcoming(clock.instant(), limit).map { doses ->
            doses.map { it.toUpcoming() }
        }

    private fun Dose.toUpcoming(): UpcomingDose {
        val now = clock.instant()
        return UpcomingDose(
            doseId = id,
            medicationName = medicationName,
            amount = amount,
            scheduledAt = scheduledAt,
            isOverdue = scheduledAt.isBefore(now),
            snoozedUntil = snoozedUntil,
        )
    }
}
