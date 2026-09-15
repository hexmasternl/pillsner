package nl.hexmaster.pillsner.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.RoomDoseRepository
import nl.hexmaster.pillsner.data.RoomMedicationRepository
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.PlannedDose
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: dose-records storage and medication-persistence schema version 2. */
@RunWith(AndroidJUnit4::class)
class DoseDaoTest {

    private lateinit var database: PillsnerDatabase
    private lateinit var doses: RoomDoseRepository
    private lateinit var medications: RoomMedicationRepository

    private val today: LocalDate = LocalDate.of(2026, 9, 14)
    private val morning: Instant = Instant.parse("2026-09-14T06:00:00Z")
    private val evening: Instant = Instant.parse("2026-09-14T18:00:00Z")
    private val mg40 = Quantity.of("40", DoseUnit.MILLIGRAM)

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PillsnerDatabase::class.java).build()
        doses = RoomDoseRepository(database.doseDao())
        medications = RoomMedicationRepository(database.medicationDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aPlannedDose_roundTripsWithItsSnapshot() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, morning, Quantity.of("2.5", DoseUnit.MILLILITRE))))

        val stored = doses.pending().single()
        assertEquals(id, stored.medicationId)
        assertEquals("Ibuprofen", stored.medicationName)
        assertEquals(Quantity.of("2.5", DoseUnit.MILLILITRE), stored.amount)
        assertEquals(BigDecimal("2.5"), stored.amount.value)
        assertEquals(morning, stored.scheduledAt)
        assertTrue(stored.isPending)
    }

    @Test
    fun insertingTheSamePlannedDoseTwice_changesNothing() = runBlocking {
        val id = medications.add(medication())

        doses.insertPlanned(listOf(planned(id, morning)))
        doses.insertPlanned(listOf(planned(id, morning)))

        assertEquals(1, doses.pending().size)
    }

    @Test
    fun recordingAnOutcome_takesTheDoseOutOfPendingAndClearsItsSnooze() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, morning)))
        val dose = doses.pending().single()
        doses.setSnooze(dose.id, morning.plusSeconds(900))

        doses.recordIntake(dose.id, IntakeOutcome.TAKEN, morning.plusSeconds(60))

        assertEquals(emptyList<Any>(), doses.pending())
        val stored = checkNotNull(doses.get(dose.id))
        assertEquals(IntakeOutcome.TAKEN, stored.intake?.outcome)
        assertEquals(morning.plusSeconds(60), stored.intake?.recordedAt)
        assertNull(stored.snoozedUntil)
    }

    @Test
    fun deletingTheMedication_leavesTheDoseWithoutLosingItsHistory() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, morning)))

        // Nothing in the app deletes a medicine; the nullable reference is what would keep the
        // history readable if a row ever did vanish, so it is exercised through raw SQL.
        database.openHelper.writableDatabase.execSQL("DELETE FROM medications WHERE id = ${id.value}")

        val stored = doses.pending().single()
        assertNull(stored.medicationId)
        assertEquals("Ibuprofen", stored.medicationName)
        assertEquals(mg40, stored.amount)
    }

    @Test
    fun withdrawingPlannedDoses_leavesRemindedAndAnsweredOnesAlone() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(
            listOf(planned(id, morning), planned(id, evening), planned(id, evening.plusSeconds(3600))),
        )
        val all = doses.pending()
        doses.recordReminded(all[0].id, morning, countsAsRepeat = false)
        doses.recordIntake(all[1].id, IntakeOutcome.SKIPPED, evening)

        val withdrawn = doses.withdrawPlanned(
            from = morning.minusSeconds(86_400),
            to = evening.plusSeconds(86_400),
            planned = mapOf(id to emptyList()),
            includeReminded = false,
        )

        // The reminded one survives; the answered one is not pending; the merely planned one is gone.
        assertEquals(listOf(morning), doses.pending().map { it.scheduledAt })
        assertEquals(IntakeOutcome.SKIPPED, doses.get(all[1].id)?.intake?.outcome)
        assertEquals(listOf(all[2].id), withdrawn)
    }

    @Test
    fun withdrawingPlannedDoses_takesARemindedOneWhenTheUserChangedTheMedicine() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, morning), planned(id, evening)))
        val all = doses.pending()
        doses.recordReminded(all[0].id, morning, countsAsRepeat = false)
        doses.recordIntake(all[1].id, IntakeOutcome.TAKEN, evening)

        val withdrawn = doses.withdrawPlanned(
            from = morning.minusSeconds(86_400),
            to = evening.plusSeconds(86_400),
            planned = mapOf(id to emptyList()),
            includeReminded = true,
        )

        // The reminded one goes; the answered one is history and stays under either mode.
        assertEquals(listOf(all[0].id), withdrawn)
        assertEquals(emptyList<Instant>(), doses.pending().map { it.scheduledAt })
        assertEquals(IntakeOutcome.TAKEN, doses.get(all[1].id)?.intake?.outcome)
    }

    @Test
    fun withdrawingPlannedDoses_keepsTheOnesStillCalledFor() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, morning), planned(id, evening)))

        doses.withdrawPlanned(
            from = morning.minusSeconds(86_400),
            to = evening.plusSeconds(86_400),
            planned = mapOf(id to listOf(evening)),
            includeReminded = false,
        )

        assertEquals(listOf(evening), doses.pending().map { it.scheduledAt })
    }

    @Test
    fun withdrawingPlannedDoses_matchesOnTheMedicineAndNotOnTheMomentAlone() = runBlocking {
        val moved = medications.add(medication())
        val unchanged = medications.add(medication())
        doses.insertPlanned(listOf(planned(moved, morning), planned(unchanged, morning)))

        // One medicine moves off the morning; the other still takes its dose then.
        doses.withdrawPlanned(
            from = morning.minusSeconds(86_400),
            to = evening.plusSeconds(86_400),
            planned = mapOf(moved to listOf(evening), unchanged to listOf(morning)),
            includeReminded = false,
        )

        assertEquals(
            listOf(unchanged to morning),
            doses.pending().map { it.medicationId to it.scheduledAt },
        )
    }

    @Test
    fun withdrawingPlannedDoses_neverTakesADoseWhoseMedicineIsGone() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, morning)))
        // The reference goes null, as in deletingTheMedication_leavesTheDoseWithoutLosingItsHistory.
        database.openHelper.writableDatabase.execSQL("DELETE FROM medications WHERE id = ${id.value}")

        val withdrawn = doses.withdrawPlanned(
            from = morning.minusSeconds(86_400),
            to = evening.plusSeconds(86_400),
            planned = mapOf(id to emptyList()),
            includeReminded = true,
        )

        assertEquals(emptyList<Any>(), withdrawn)
        assertEquals(listOf(morning), doses.pending().map { it.scheduledAt })
    }

    @Test
    fun refreshingSnapshots_updatesAPendingDoseAndLeavesAnAnsweredOneAlone() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, morning), planned(id, evening)))
        val answered = doses.pending().first { it.scheduledAt == morning }
        doses.recordIntake(answered.id, IntakeOutcome.TAKEN, morning)

        val renamed = Quantity.of("1", DoseUnit.TABLET)
        doses.refreshSnapshots(
            listOf(
                PlannedDose(id, "Ibuprofen 400", renamed, morning),
                PlannedDose(id, "Ibuprofen 400", renamed, evening),
            ),
        )

        val stillPending = doses.pending().single()
        assertEquals("Ibuprofen 400", stillPending.medicationName)
        assertEquals(renamed, stillPending.amount)

        val history = doses.get(answered.id)!!
        assertEquals("Ibuprofen", history.medicationName)
        assertEquals(mg40, history.amount)
    }

    @Test
    fun theNextDoseOfAMedicine_isFound() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, morning), planned(id, evening)))

        assertEquals(evening, doses.nextScheduledAtAfter(id, morning))
        assertNull(doses.nextScheduledAtAfter(id, evening))
    }

    @Test
    fun thePendingStream_reEmitsWhenADoseIsAdded() = runBlocking {
        val id = medications.add(medication())
        assertEquals(emptyList<Any>(), doses.observePending().first())

        doses.insertPlanned(listOf(planned(id, morning)))

        assertEquals(listOf(morning), doses.observePending().first().map { it.scheduledAt })
    }

    @Test
    fun thePendingStream_reEmitsWhenASnapshotIsRefreshed() = runBlocking {
        // This is what carries a rename to the Home screen while the user is looking at it.
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, morning)))
        assertEquals("Ibuprofen", doses.observePending().first().single().medicationName)

        doses.refreshSnapshots(listOf(PlannedDose(id, "Ibuprofen 400", mg40, morning)))

        assertEquals("Ibuprofen 400", doses.observePending().first().single().medicationName)
    }

    @Test
    fun pendingDosesComeBackSoonestFirst() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, evening), planned(id, morning)))

        assertEquals(listOf(morning, evening), doses.pending().map { it.scheduledAt })
    }

    @Test
    fun theHistoryOfOneMedicine_holdsOnlyItsOwnDosesInsideTheRange() = runBlocking {
        val mine = medications.add(medication())
        val other = medications.add(medication())
        val earlier = morning.minusSeconds(86_400)
        val later = evening.plusSeconds(86_400)
        doses.insertPlanned(
            listOf(
                planned(mine, earlier),
                planned(mine, morning),
                planned(mine, evening),
                planned(mine, later),
                planned(other, morning),
            ),
        )
        // One answered and one still unanswered, so both kinds have to come back.
        doses.recordIntake(doses.pending().first { it.scheduledAt == morning && it.medicationId == mine }.id, IntakeOutcome.TAKEN, morning)

        val history = doses.observeHistoryFor(mine, morning, later).first()

        assertEquals(listOf(morning, evening), history.map { it.scheduledAt })
        assertEquals(IntakeOutcome.TAKEN, history[0].intake?.outcome)
        assertNull(history[1].intake)
    }

    @Test
    fun theEarliestRecordedMoment_isTheOldestStoredDose() = runBlocking {
        val id = medications.add(medication())
        val withoutDoses = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, evening), planned(id, morning)))

        assertEquals(morning, doses.earliestScheduledAt(id))
        assertNull(doses.earliestScheduledAt(withoutDoses))
    }

    @Test
    fun observingOneDose_followsItUntilItIsGone() = runBlocking {
        val medicationId = medications.add(medication())
        doses.insertPlanned(listOf(planned(medicationId, morning)))
        val id = doses.pending().single().id

        assertEquals(morning, doses.observe(id).first()?.scheduledAt)

        doses.recordIntake(id, IntakeOutcome.TAKEN, morning)
        assertEquals(IntakeOutcome.TAKEN, doses.observe(id).first()?.intake?.outcome)

        // Withdrawn by a refresh after the user changed the medicine: the stream says so with null
        // rather than stalling on the last thing it saw.
        database.doseDao().deleteByIds(listOf(id.value))
        assertNull(doses.observe(id).first())
    }

    @Test
    fun aFreshDose_hasNotBeenAskedAboutAgain() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, morning)))

        val fresh = doses.pending().single()
        assertEquals(0, fresh.reminderCount)
        assertNull(fresh.lastRemindedAt)
    }

    @Test
    fun aRepeatMovesTheAnchorButNeverTheMomentTheUserWasFirstTold() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, morning)))
        val dose = doses.pending().single()

        doses.recordReminded(dose.id, morning, countsAsRepeat = false)
        doses.recordReminded(dose.id, morning.plusSeconds(900), countsAsRepeat = true)

        val repeated = doses.pending().single()
        assertEquals(morning, repeated.firstRemindedAt)
        assertEquals(morning.plusSeconds(900), repeated.lastRemindedAt)
        assertEquals(1, repeated.reminderCount)
    }

    @Test
    fun eachRepeatCountsOnce() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, morning)))
        val dose = doses.pending().single()

        doses.recordReminded(dose.id, morning, countsAsRepeat = true)
        doses.recordReminded(dose.id, morning, countsAsRepeat = true)

        assertEquals(2, doses.pending().single().reminderCount)
    }

    @Test
    fun aSnoozePutsTheRepeatsBackToTheStart() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, morning)))
        val dose = doses.pending().single()
        repeat(3) { doses.recordReminded(dose.id, morning, countsAsRepeat = true) }

        // "Not yet" is an acknowledgement, so the repeats before it must not count against the user.
        doses.setSnooze(dose.id, evening)

        val snoozed = doses.pending().single()
        assertEquals(0, snoozed.reminderCount)
        assertEquals(evening, snoozed.snoozedUntil)
    }

    @Test
    fun theRepeatCountBelongsToOneDoseOnly() = runBlocking {
        val id = medications.add(medication())
        doses.insertPlanned(listOf(planned(id, morning), planned(id, evening)))
        val first = doses.pending().first()

        doses.recordReminded(first.id, morning, countsAsRepeat = true)

        val stored = doses.pending().associateBy { it.scheduledAt }
        assertEquals(1, stored.getValue(morning).reminderCount)
        assertEquals(0, stored.getValue(evening).reminderCount)
    }

    private fun medication() = NewMedication(
        name = "Ibuprofen",
        defaultDose = mg40,
        usedSince = today,
        useUntil = null,
        prescribedBy = Prescriber.GENERAL_PRACTITIONER,
        schedules = emptyList(),
    )

    private fun planned(id: MedicationId, at: Instant, amount: Quantity = mg40) = PlannedDose(
        medicationId = id,
        medicationName = "Ibuprofen",
        amount = amount,
        scheduledAt = at,
    )
}
