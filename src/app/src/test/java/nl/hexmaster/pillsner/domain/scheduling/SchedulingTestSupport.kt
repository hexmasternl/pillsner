package nl.hexmaster.pillsner.domain.scheduling

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.TestFixtures.mg40

/** Fixtures shared by the scheduling and intake tests, so each test reads as what it is about. */
object SchedulingTestSupport {

    val amsterdam: ZoneId = ZoneId.of("Europe/Amsterdam")

    /** A Monday, so weekday schedules are easy to reason about. */
    val today: LocalDate = LocalDate.of(2026, 9, 14)

    fun at(date: LocalDate = today, hour: Int, minute: Int = 0, zone: ZoneId = amsterdam): Instant =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), zone).toInstant()

    /**
     * [lastRemindedAt] defaults to [firstRemindedAt], which is what a dose announced once and never
     * repeated looks like; a test about the repeat rule moves it on its own.
     */
    fun dose(
        id: Long,
        scheduledAt: Instant,
        medicationId: Long? = 1L,
        firstRemindedAt: Instant? = null,
        snoozedUntil: Instant? = null,
        lastRemindedAt: Instant? = firstRemindedAt,
        plannedAt: Instant = scheduledAt.minus(Duration.ofHours(12)),
        reminderCount: Int = 0,
    ) = Dose(
        id = DoseId(id),
        medicationId = medicationId?.let(::MedicationId),
        medicationName = "Ibuprofen",
        amount = mg40,
        scheduledAt = scheduledAt,
        plannedAt = plannedAt,
        firstRemindedAt = firstRemindedAt,
        snoozedUntil = snoozedUntil,
        lastRemindedAt = lastRemindedAt,
        reminderCount = reminderCount,
    )

    fun repositoryWith(vararg doses: Dose) = InMemoryDoseRepository(doses.toList())
}
