package nl.hexmaster.pillsner.domain.scheduling

import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.model.TestFixtures.medication
import nl.hexmaster.pillsner.domain.model.TestFixtures.mg40
import nl.hexmaster.pillsner.domain.model.TestFixtures.oneTablet
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.amsterdam
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.at
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.today
import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec: dose-records window refresh. */
class RefreshPlannedDosesTest {

    private val clock = MutableTestClock(at(hour = 6), amsterdam)
    private val medications = InMemoryMedicationRepository()
    private val doses = InMemoryDoseRepository()
    private val refresh = RefreshPlannedDoses(medications, doses, DoseGenerator(), clock)

    private val twiceADay = medication(
        usedSince = today,
        schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))),
    )

    @Test
    fun `a new medicine gets two days of doses`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))

        refresh()

        assertEquals(
            listOf(at(hour = 8), at(hour = 20), at(today.plusDays(1), 8), at(today.plusDays(1), 20)),
            doses.all().map { it.scheduledAt },
        )
    }

    @Test
    fun `running twice changes nothing`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()
        val first = doses.all()

        refresh()

        assertEquals(first, doses.all())
    }

    @Test
    fun `removing a schedule withdraws its planned doses`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()

        medications.update(MedicationId(1)) {
            it.copy(schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(8, 0)))))
        }
        refresh()

        assertEquals(
            listOf(at(hour = 8), at(today.plusDays(1), 8)),
            doses.all().map { it.scheduledAt },
        )
    }

    @Test
    fun `deactivating a medicine withdraws its planned doses`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()

        medications.update(MedicationId(1)) { it.copy(isActive = false) }
        refresh()

        assertEquals(emptyList<Instant>(), doses.all().map { it.scheduledAt })
    }

    @Test
    fun `a clock change never withdraws a dose the user has been reminded about`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()
        val morning = doses.all().first()
        doses.recordReminded(morning.id, at(hour = 8), countsAsRepeat = false)

        medications.update(MedicationId(1)) { it.copy(isActive = false) }
        refresh()

        assertEquals(listOf(morning.scheduledAt), doses.all().map { it.scheduledAt })
    }

    @Test
    fun `the user's own edit withdraws a dose they have been reminded about`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()
        val morning = doses.all().first()
        doses.recordReminded(morning.id, at(hour = 8), countsAsRepeat = false)

        medications.update(MedicationId(1)) {
            it.copy(schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(20, 0)))))
        }
        val withdrawn = refresh(afterUserEdit = true)

        // The reminded 08:00 dose goes with the rest of what the new schedule no longer calls for.
        assertEquals(true, morning.id in withdrawn)
        assertEquals(
            listOf(at(hour = 20), at(today.plusDays(1), 20)),
            doses.all().map { it.scheduledAt },
        )
    }

    @Test
    fun `an edit never withdraws a dose the user has answered`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()
        val morning = doses.all().first()
        doses.recordIntake(morning.id, IntakeOutcome.TAKEN, at(hour = 8))

        medications.update(MedicationId(1)) { it.copy(isActive = false) }
        val withdrawn = refresh(afterUserEdit = true)

        // The other three doses go; the answered one is the user's record and stays.
        assertEquals(false, morning.id in withdrawn)
        assertEquals(listOf(morning.scheduledAt), doses.all().map { it.scheduledAt })
    }

    @Test
    fun `an edit leaves an outstanding reminder from before the window alone`() = runBlocking {
        // Yesterday evening's dose is unanswered and the user has been told about it. It is outside
        // the two-day window only because the window does not reach back that far, not because the
        // user stopped taking it, so an edit must not take it away.
        val yesterday = SchedulingTestSupport.dose(
            id = 99,
            scheduledAt = at(today.minusDays(1), 23),
            firstRemindedAt = at(today.minusDays(1), 23),
        )
        val doses = InMemoryDoseRepository(listOf(yesterday))
        val refresh = RefreshPlannedDoses(medications, doses, DoseGenerator(), clock)
        medications.replaceAll(listOf(twiceADay))

        val withdrawn = refresh(afterUserEdit = true)

        assertEquals(emptyList<DoseId>(), withdrawn)
        assertEquals(true, doses.all().any { it.id == yesterday.id })
    }

    @Test
    fun `withdrawal leaves another medicine's dose at the same moment alone`() = runBlocking {
        val other = medication(
            id = 2,
            name = "Paracetamol",
            usedSince = today,
            schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(8, 0)))),
        )
        medications.replaceAll(listOf(twiceADay, other))
        refresh()

        // Ibuprofen moves off 08:00; Paracetamol stays on it.
        medications.update(MedicationId(1)) {
            it.copy(schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(9, 0)))))
        }
        refresh()

        assertEquals(
            listOf(
                MedicationId(2) to at(hour = 8),
                MedicationId(1) to at(hour = 9),
                MedicationId(2) to at(today.plusDays(1), 8),
                MedicationId(1) to at(today.plusDays(1), 9),
            ),
            doses.all().map { it.medicationId to it.scheduledAt },
        )
    }

    @Test
    fun `renaming a medicine reaches its pending doses`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()

        medications.update(MedicationId(1)) { it.copy(name = "Ibuprofen 400") }
        refresh()

        assertEquals(
            listOf("Ibuprofen 400", "Ibuprofen 400", "Ibuprofen 400", "Ibuprofen 400"),
            doses.all().map { it.medicationName },
        )
    }

    @Test
    fun `changing the amount reaches its pending doses`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()

        medications.update(MedicationId(1)) {
            it.copy(schedules = listOf(Schedule.EveryNDays(oneTablet, 1, listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))))
        }
        refresh()

        assertEquals(listOf(oneTablet, oneTablet, oneTablet, oneTablet), doses.all().map { it.amount })
    }

    @Test
    fun `an answered dose keeps the name and amount it was recorded with`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()
        val morning = doses.all().first()
        doses.recordIntake(morning.id, IntakeOutcome.TAKEN, at(hour = 8))

        medications.update(MedicationId(1)) {
            it.copy(
                name = "Ibuprofen 400",
                schedules = listOf(Schedule.EveryNDays(oneTablet, 1, listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))),
            )
        }
        refresh()

        val answered = doses.all().first { it.id == morning.id }
        assertEquals("Ibuprofen", answered.medicationName)
        assertEquals(mg40, answered.amount)
    }

    @Test
    fun `a refresh that withdraws nothing reports nothing`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))

        assertEquals(emptyList<DoseId>(), refresh())
    }

    @Test
    fun `an answered dose is never withdrawn`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()
        val morning = doses.all().first()
        doses.recordIntake(morning.id, IntakeOutcome.TAKEN, at(hour = 8))

        medications.update(MedicationId(1)) { it.copy(isActive = false) }
        refresh()

        assertEquals(listOf(morning.scheduledAt), doses.all().map { it.scheduledAt })
    }

    @Test
    fun `the day rolling over plans the new tomorrow`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()

        clock.setTo(at(today.plusDays(1), 0, 5))
        refresh()

        assertEquals(
            listOf(
                at(today.plusDays(1), 8),
                at(today.plusDays(1), 20),
                at(today.plusDays(2), 8),
                at(today.plusDays(2), 20),
            ),
            doses.all().map { it.scheduledAt },
        )
    }

    @Test
    fun `moving time zone re-anchors the planned doses to the new wall clock`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()
        val here = doses.all().map { it.scheduledAt }

        clock.moveTo(java.time.ZoneId.of("Asia/Tokyo"))
        refresh()

        val there = doses.all().map { it.scheduledAt }
        assertEquals(4, there.size)
        assertEquals(emptyList<Instant>(), there.filter { it in here })
        // Eight in the morning stays eight in the morning, wherever the user woke up.
        assertEquals(
            at(today, 8, zone = java.time.ZoneId.of("Asia/Tokyo")),
            there.first(),
        )
    }
}
