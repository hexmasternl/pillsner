package nl.hexmaster.pillsner.domain.model

import java.time.LocalTime
import nl.hexmaster.pillsner.domain.model.TestFixtures.everyDay
import nl.hexmaster.pillsner.domain.model.TestFixtures.medication
import nl.hexmaster.pillsner.domain.model.TestFixtures.mg40
import nl.hexmaster.pillsner.domain.model.TestFixtures.newMedication
import nl.hexmaster.pillsner.domain.model.TestFixtures.startDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Spec: Medication domain model. */
class MedicationTest {

    @Test
    fun twoSchedules_areKeptInTheOrderGiven() {
        val first = everyDay(8)
        val second = Schedule.EveryNHours(mg40, 12, LocalTime.of(20, 0))

        val subject = medication(schedules = listOf(first, second))

        assertEquals(listOf(first, second), subject.schedules)
    }

    @Test
    fun noSchedules_isValid() {
        assertEquals(emptyList<Schedule>(), medication(schedules = emptyList()).schedules)
    }

    @Test
    fun aBlankName_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { medication(name = "   ") }
    }

    @Test
    fun useUntilBeforeUsedSince_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            medication(usedSince = startDate, useUntil = startDate.minusDays(1))
        }
    }

    @Test
    fun useUntilOnUsedSince_isAccepted() {
        assertEquals(startDate, medication(usedSince = startDate, useUntil = startDate).useUntil)
    }

    @Test
    fun aNewMedication_appliesTheSameRules() {
        assertThrows(IllegalArgumentException::class.java) { newMedication(name = "") }
        assertThrows(IllegalArgumentException::class.java) {
            newMedication(usedSince = startDate, useUntil = startDate.minusDays(1))
        }
    }

    @Test
    fun prescriberIsRecorded() {
        assertEquals(Prescriber.SELF, medication(prescribedBy = Prescriber.SELF).prescribedBy)
    }
}
