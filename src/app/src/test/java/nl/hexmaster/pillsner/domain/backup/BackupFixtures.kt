package nl.hexmaster.pillsner.domain.backup

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Intake
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.LowStockAcknowledgement
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.model.TestFixtures

/** A snapshot with something of every kind in it, so a round trip has everything to lose. */
object BackupFixtures {

    private val morning: Instant = Instant.parse("2026-09-14T06:00:00Z")

    fun populated(): BackupSnapshot = BackupSnapshot(
        medications = listOf(
            BackupMedication(
                backupId = "a",
                name = "Ibuprofen",
                defaultDose = TestFixtures.mg40,
                usedSince = TestFixtures.startDate,
                useUntil = null,
                prescribedBy = Prescriber.GENERAL_PRACTITIONER,
                schedules = listOf(
                    TestFixtures.everyDay(8, 20),
                    Schedule.OnWeekdays(TestFixtures.oneTablet, setOf(DayOfWeek.MONDAY), listOf(TestFixtures.time(9, 30))),
                    Schedule.EveryNHours(Quantity.of("2.5", DoseUnit.MILLILITRE), 8, TestFixtures.time(7)),
                ),
                isActive = true,
                lowStockAcknowledgement = LowStockAcknowledgement.ACKNOWLEDGED_ORDERED,
            ),
            BackupMedication(
                backupId = "b",
                name = "Vitamin D",
                defaultDose = TestFixtures.oneTablet,
                usedSince = TestFixtures.startDate,
                useUntil = LocalDate.of(2026, 12, 31),
                prescribedBy = Prescriber.SELF,
                schedules = emptyList(),
                isActive = false,
                lowStockAcknowledgement = null,
            ),
        ),
        stockBatches = listOf(
            BackupStockBatch("a", BigDecimal("49.92"), DoseUnit.TABLET, BigDecimal("20"), LocalDate.of(2027, 3, 1), morning),
            BackupStockBatch("b", BigDecimal("0"), DoseUnit.TABLET, BigDecimal.ONE, LocalDate.of(2026, 11, 1), morning),
        ),
        doses = listOf(
            dose("a", morning, Intake(IntakeOutcome.TAKEN, morning.plusSeconds(120))),
            dose("a", morning.plusSeconds(3600), Intake(IntakeOutcome.SKIPPED, morning.plusSeconds(3700))),
            dose("a", morning.plusSeconds(7200), Intake(IntakeOutcome.MISSED, morning.plusSeconds(9000))),
            // Pending, snoozed, reminded twice.
            dose("a", morning.plusSeconds(10800), null).copy(
                snoozedUntil = morning.plusSeconds(11400),
                firstRemindedAt = morning.plusSeconds(10800),
                lastRemindedAt = morning.plusSeconds(11000),
                reminderCount = 2,
            ),
            // Its medicine was already gone at export time.
            dose(null, morning.minusSeconds(86_400), Intake(IntakeOutcome.TAKEN, morning.minusSeconds(86_000))),
        ),
    )

    fun dose(medicationBackupId: String?, scheduledAt: Instant, intake: Intake?) = BackupDose(
        medicationBackupId = medicationBackupId,
        medicationName = if (medicationBackupId == null) "Paracetamol" else "Ibuprofen",
        amount = TestFixtures.mg40,
        scheduledAt = scheduledAt,
        plannedAt = scheduledAt.minusSeconds(43_200),
        intake = intake,
        snoozedUntil = null,
        firstRemindedAt = null,
        lastRemindedAt = null,
        reminderCount = 0,
    )
}
