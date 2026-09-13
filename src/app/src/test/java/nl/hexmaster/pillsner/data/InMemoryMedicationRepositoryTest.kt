package nl.hexmaster.pillsner.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.TestFixtures.everyDay
import nl.hexmaster.pillsner.domain.model.TestFixtures.medication
import nl.hexmaster.pillsner.domain.model.TestFixtures.newMedication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec: Medication repository contract. */
@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryMedicationRepositoryTest {

    private val ibuprofen = medication()

    @Test
    fun `the first emission is empty`() = runTest {
        val repository = InMemoryMedicationRepository()

        assertEquals(emptyList<Medication>(), repository.observeAll().first())
    }

    @Test
    fun `adding a medication re-emits to an active collector`() = runTest(UnconfinedTestDispatcher()) {
        val repository = InMemoryMedicationRepository()
        val seen = mutableListOf<List<Medication>>()
        val job = backgroundScope.launch { repository.observeAll().collect { seen += it } }

        repository.add(newMedication(name = "Ibuprofen", schedules = listOf(everyDay(8))))

        assertEquals(2, seen.size)
        assertEquals(listOf("Ibuprofen"), seen.last().map { it.name })
        job.cancel()
    }

    @Test
    fun `add returns the identifier the medication appears under`() = runTest(UnconfinedTestDispatcher()) {
        val repository = InMemoryMedicationRepository()

        val id = repository.add(newMedication(name = "Ibuprofen"))

        assertEquals(id, repository.observeAll().first().single().id)
    }

    @Test
    fun `an added medication is active`() = runTest(UnconfinedTestDispatcher()) {
        val repository = InMemoryMedicationRepository()

        repository.add(newMedication())

        assertTrue(repository.observeAll().first().single().isActive)
    }

    @Test
    fun `an added medication keeps its schedules in order`() = runTest(UnconfinedTestDispatcher()) {
        val repository = InMemoryMedicationRepository()
        val schedules = listOf(everyDay(8), everyDay(12, 20))

        repository.add(newMedication(schedules = schedules))

        assertEquals(schedules, repository.observeAll().first().single().schedules)
    }

    @Test
    fun `upsert replaces a medication with the same identifier`() = runTest(UnconfinedTestDispatcher()) {
        val repository = InMemoryMedicationRepository(listOf(ibuprofen))

        repository.upsert(ibuprofen.copy(name = "Ibuprofen retard"))

        val medications = repository.observeAll().first()
        assertEquals(1, medications.size)
        assertEquals("Ibuprofen retard", medications.single().name)
    }

    @Test
    fun `update applies a change to one medication`() = runTest(UnconfinedTestDispatcher()) {
        val repository = InMemoryMedicationRepository(listOf(ibuprofen))

        repository.update(MedicationId(1)) { it.copy(isActive = false) }

        assertEquals(false, repository.observeAll().first().single().isActive)
    }

    @Test
    fun `setActive flips the flag and leaves everything else alone`() = runTest(UnconfinedTestDispatcher()) {
        val withSchedules = medication(schedules = listOf(everyDay(8), everyDay(12, 20)))
        val repository = InMemoryMedicationRepository(listOf(withSchedules))

        repository.setActive(withSchedules.id, isActive = false)

        val stored = repository.observeAll().first().single()
        assertEquals(false, stored.isActive)
        assertEquals(withSchedules.copy(isActive = false), stored)
    }

    @Test
    fun `setActive re-emits to an active collector`() = runTest(UnconfinedTestDispatcher()) {
        val repository = InMemoryMedicationRepository(listOf(ibuprofen))
        val seen = mutableListOf<List<Medication>>()
        val job = backgroundScope.launch { repository.observeAll().collect { seen += it } }

        repository.setActive(ibuprofen.id, isActive = false)

        assertEquals(2, seen.size)
        assertEquals(false, seen.last().single().isActive)
        job.cancel()
    }

    @Test
    fun `get finds a medication by its identifier`() = runTest(UnconfinedTestDispatcher()) {
        val repository = InMemoryMedicationRepository(listOf(ibuprofen))

        assertEquals(ibuprofen, repository.get(MedicationId(1)))
    }

    @Test
    fun `get on an unknown medicine finds nothing`() = runTest(UnconfinedTestDispatcher()) {
        val repository = InMemoryMedicationRepository(listOf(ibuprofen))

        assertEquals(null, repository.get(MedicationId(404)))
    }

    @Test
    fun `update replaces the whole medicine under the same identifier`() = runTest(UnconfinedTestDispatcher()) {
        val repository = InMemoryMedicationRepository(listOf(ibuprofen))
        val edited = ibuprofen.copy(name = "Ibuprofen retard", schedules = listOf(everyDay(9, 21)), isActive = false)

        repository.update(edited)

        assertEquals(listOf(edited), repository.observeAll().first())
    }

    @Test
    fun `update emits once`() = runTest(UnconfinedTestDispatcher()) {
        val repository = InMemoryMedicationRepository(listOf(ibuprofen))
        val seen = mutableListOf<List<Medication>>()
        val job = backgroundScope.launch { repository.observeAll().collect { seen += it } }

        repository.update(ibuprofen.copy(name = "Ibuprofen retard"))

        assertEquals(2, seen.size)
        job.cancel()
    }

    @Test
    fun `update on an unknown medicine fails and stores nothing`() = runTest(UnconfinedTestDispatcher()) {
        val repository = InMemoryMedicationRepository(listOf(ibuprofen))

        assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.update(ibuprofen.copy(id = MedicationId(404))) }
        }

        assertEquals(listOf(ibuprofen), repository.observeAll().first())
    }

    @Test
    fun `setActive on an unknown medicine changes nothing`() = runTest(UnconfinedTestDispatcher()) {
        val repository = InMemoryMedicationRepository(listOf(ibuprofen))

        repository.setActive(MedicationId(404), isActive = false)

        assertEquals(listOf(ibuprofen), repository.observeAll().first())
    }
}
