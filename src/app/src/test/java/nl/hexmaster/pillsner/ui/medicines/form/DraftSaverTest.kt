package nl.hexmaster.pillsner.ui.medicines.form

import androidx.lifecycle.SavedStateHandle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec: Draft survives configuration changes. */
class DraftSaverTest {

    private val today = LocalDate.of(2026, 9, 13)
    private val mg40 = Quantity.of("40", DoseUnit.MILLIGRAM)

    @Test
    fun `an untouched handle restores a fresh draft starting today`() {
        val restored = DraftSaver.restore(SavedStateHandle(), today)

        assertEquals(MedicationFormDraft(usedSince = today), restored)
    }

    @Test
    fun `a full draft survives a save and restore`() {
        val handle = SavedStateHandle()
        val draft = MedicationFormDraft(
            name = "Metoprolol",
            doseText = "2.5",
            doseUnit = DoseUnit.MILLILITRE,
            usedSince = today,
            useUntil = today.plusDays(30),
            prescribedBy = Prescriber.SPECIALIST,
            schedules = listOf(
                Schedule.EveryNDays(mg40, 2, listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))),
                Schedule.OnWeekdays(
                    mg40,
                    setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                    listOf(LocalTime.of(9, 30)),
                ),
                Schedule.EveryNHours(mg40, 12, LocalTime.of(7, 0)),
            ),
        )

        DraftSaver.save(handle, draft)

        assertEquals(draft, DraftSaver.restore(handle, today))
    }

    @Test
    fun `an absent end date stays absent`() {
        val handle = SavedStateHandle()
        val draft = MedicationFormDraft(name = "Ibuprofen", usedSince = today, useUntil = null)

        DraftSaver.save(handle, draft)

        assertEquals(null, DraftSaver.restore(handle, today).useUntil)
    }

    @Test
    fun `a decimal amount keeps its exact value through the round trip`() {
        val handle = SavedStateHandle()
        val amount = Quantity.of("2.500", DoseUnit.MILLILITRE)
        val draft = MedicationFormDraft(
            usedSince = today,
            schedules = listOf(Schedule.EveryNHours(amount, 8, LocalTime.of(7, 0))),
        )

        DraftSaver.save(handle, draft)

        val restored = DraftSaver.restore(handle, today).schedules.single()
        assertEquals(amount, restored.amount)
        assertEquals("2.500", restored.amount.value.toPlainString())
    }
}
