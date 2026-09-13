package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.PillsnerApplication
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.PlannedDose
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The answers a user gives from the notification, delivered the way Android delivers them
 * (spec: medicine-reminders responses).
 *
 * The receiver is driven directly rather than through a real broadcast, because what matters here
 * is that the intent it receives records the right outcome; that the intent reaches it at all is
 * the manifest's job.
 *
 * Doses are planted a week out, beyond the app's own planning window, so the coordinator's wake
 * neither announces nor withdraws them while the test is running.
 */
@RunWith(AndroidJUnit4::class)
class ReminderActionReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var doses: DoseRepository
    private lateinit var medications: MedicationRepository
    private var medicationId: MedicationId = MedicationId(0)

    @Before
    fun setUp() = runBlocking {
        val container = (context.applicationContext as PillsnerApplication).container
        doses = container.doseRepository
        medications = container.medicationRepository
        medicationId = medications.add(
            NewMedication(
                name = "Ibuprofen",
                defaultDose = Quantity.of("40", DoseUnit.MILLIGRAM),
                usedSince = LocalDate.now(),
                useUntil = null,
                prescribedBy = Prescriber.SELF,
                // No schedules, so nothing this test plants is ever regenerated or withdrawn.
                schedules = emptyList(),
            ),
        )
    }

    @Test
    fun tookIt_recordsTheDoseAsTaken() = runBlocking {
        val id = plantDose(daysAhead = 7)

        deliver(id, ReminderAction.TAKEN)

        val dose = checkNotNull(doses.get(id))
        assertEquals(IntakeOutcome.TAKEN, dose.intake?.outcome)
        assertNotNull(dose.intake?.recordedAt)
    }

    @Test
    fun notGoingTo_recordsTheDoseAsSkipped() = runBlocking {
        val id = plantDose(daysAhead = 8)

        deliver(id, ReminderAction.SKIP)

        assertEquals(IntakeOutcome.SKIPPED, doses.get(id)?.intake?.outcome)
    }

    @Test
    fun notYet_postponesTheDoseWithoutAnsweringIt() = runBlocking {
        val id = plantDose(daysAhead = 9)

        deliver(id, ReminderAction.SNOOZE)

        val dose = checkNotNull(doses.get(id))
        assertNull(dose.intake)
        assertNotNull(dose.snoozedUntil)
    }

    /** Drives the receiver and waits for the work it hands to a coroutine. */
    private suspend fun deliver(id: DoseId, action: ReminderAction) {
        ReminderActionReceiver().onReceive(context, ReminderActionReceiver.intent(context, id, action))
        var attempts = 0
        while (attempts < MAX_ATTEMPTS && !answered(id, action)) {
            delay(POLL_MILLIS)
            attempts++
        }
    }

    private suspend fun answered(id: DoseId, action: ReminderAction): Boolean {
        val dose = doses.get(id) ?: return false
        return when (action) {
            ReminderAction.SNOOZE -> dose.snoozedUntil != null
            else -> dose.intake != null
        }
    }

    private suspend fun plantDose(daysAhead: Long): DoseId {
        val at = Instant.now().plus(daysAhead, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES)
        doses.insertPlanned(
            listOf(
                PlannedDose(
                    medicationId = medicationId,
                    medicationName = "Ibuprofen",
                    amount = Quantity.of("40", DoseUnit.MILLIGRAM),
                    scheduledAt = at,
                ),
            ),
        )
        return doses.observePending().first().first { it.scheduledAt == at }.id
    }

    private companion object {
        const val POLL_MILLIS = 50L
        const val MAX_ATTEMPTS = 100
    }
}
