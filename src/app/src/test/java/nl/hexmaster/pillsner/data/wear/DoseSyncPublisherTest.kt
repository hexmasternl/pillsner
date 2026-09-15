package nl.hexmaster.pillsner.data.wear

import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.PlannedDose
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.shared.wear.SyncedDoses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec: the phone publishes its pending doses to the watch whenever they change, in the phone's
 * language. The Data Layer itself is not exercised here — a [SyncTarget] is one method precisely so
 * that what gets published can be read back in a plain unit test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DoseSyncPublisherTest {

    private val now = Instant.parse("2026-09-13T08:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val repository = InMemoryDoseRepository()

    private val published = mutableListOf<SyncedDoses>()
    private val target = SyncTarget { json -> published += checkNotNull(SyncedDoses.decode(json)) }

    private fun publisher(
        target: SyncTarget? = this.target,
        languageTag: String = "en",
    ) = DoseSyncPublisher(
        doseRepository = repository,
        target = target,
        amountText = { quantity -> "${quantity.value.toPlainString()} ${quantity.unit.wire(languageTag)}" },
        languageTag = { languageTag },
        clock = clock,
    )

    private suspend fun plan(
        name: String,
        minutesFromNow: Long,
        amount: BigDecimal = BigDecimal("400"),
        unit: DoseUnit = DoseUnit.MILLIGRAM,
    ) {
        repository.insertPlanned(
            doses = listOf(
                PlannedDose(
                    medicationId = MedicationId(1),
                    medicationName = name,
                    amount = Quantity(amount, unit),
                    scheduledAt = now.plusSeconds(minutesFromNow * 60),
                ),
            ),
            plannedAt = now.minusSeconds(3600),
        )
    }

    @Test
    fun `every pending dose is published, soonest first`() = runTest {
        plan("Metformin", 180)
        plan("Ibuprofen", 60)

        publisher().publishNow()

        assertEquals(
            listOf("Ibuprofen", "Metformin"),
            published.single().doses.map { it.medicationName },
        )
    }

    @Test
    fun `a dose that was answered is simply not in the next list`() = runTest {
        plan("Ibuprofen", 60)
        plan("Metformin", 180)
        val taken = repository.pending().first()

        repository.recordIntake(taken.id, IntakeOutcome.TAKEN, now)
        publisher().publishNow()

        assertEquals(listOf("Metformin"), published.single().doses.map { it.medicationName })
    }

    @Test
    fun `amounts travel as the phone writes them, in the phone's language`() = runTest {
        plan("Paracetamol", 30, amount = BigDecimal("2"), unit = DoseUnit.TABLET)

        publisher(languageTag = "nl-NL").publishNow()

        val payload = published.single()
        assertEquals("2 tabletten", payload.doses.single().amountText)
        assertEquals("nl-NL", payload.languageTag)
    }

    @Test
    fun `a burst of dose changes produces one publish`() = runTest(UnconfinedTestDispatcher()) {
        publisher().start(backgroundScope)

        plan("Ibuprofen", 60)
        plan("Metformin", 120)
        plan("Simvastatin", 180)
        advanceTimeBy(1_000)

        assertEquals(1, published.size)
        assertEquals(3, published.single().doses.size)
    }

    @Test
    fun `with no watch to talk to nothing is published and nothing breaks`() = runTest {
        plan("Ibuprofen", 60)

        publisher(target = null).publishNow()

        assertTrue(published.isEmpty())
    }

    @Test
    fun `an empty list is published too, so a watch showing a dose stops showing it`() = runTest {
        publisher().publishNow()

        assertTrue(published.single().doses.isEmpty())
    }

    /** Stands in for the phone's plural resources, which a unit test has no resources for. */
    private fun DoseUnit.wire(languageTag: String): String = when (this) {
        DoseUnit.MILLIGRAM -> "mg"
        DoseUnit.TABLET -> if (languageTag.startsWith("nl")) "tabletten" else "tablets"
        else -> name.lowercase()
    }
}
