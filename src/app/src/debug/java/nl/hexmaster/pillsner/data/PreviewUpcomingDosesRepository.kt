package nl.hexmaster.pillsner.data

import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.UpcomingDose
import nl.hexmaster.pillsner.domain.repository.UpcomingDosesRepository

/**
 * Debug-only repository with three invented doses so the welcome screen can be seen with content
 * in Compose previews and on a debug device (design D5). Never wired into a release build.
 */
class PreviewUpcomingDosesRepository(
    private val now: Instant = Instant.now(),
) : UpcomingDosesRepository {

    private val sampleDoses: List<UpcomingDose> = listOf(
        UpcomingDose(DoseId(1), "Ibuprofen", Quantity.of("1", DoseUnit.TABLET), now.plus(Duration.ofHours(2))),
        UpcomingDose(DoseId(2), "Vitamin D", Quantity.of("2", DoseUnit.DROP), now.plus(Duration.ofHours(6))),
        UpcomingDose(DoseId(3), "Amoxicillin", Quantity.of("500", DoseUnit.MILLIGRAM), now.plus(Duration.ofHours(26))),
    )

    override fun observeUpcoming(limit: Int): Flow<List<UpcomingDose>> =
        flowOf(sampleDoses.sortedBy { it.scheduledAt }.take(limit))
}
