package nl.hexmaster.pillsner.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** Shared fixtures so a test says what it is about, not how to build a medication. */
object TestFixtures {

    val mg40: Quantity = Quantity.of("40", DoseUnit.MILLIGRAM)
    val oneTablet: Quantity = Quantity.of("1", DoseUnit.TABLET)
    val startDate: LocalDate = LocalDate.of(2026, 9, 13)

    fun time(hour: Int, minute: Int = 0): LocalTime = LocalTime.of(hour, minute)

    fun medication(
        id: Long = 1L,
        name: String = "Ibuprofen",
        defaultDose: Quantity = mg40,
        usedSince: LocalDate = startDate,
        useUntil: LocalDate? = null,
        prescribedBy: Prescriber = Prescriber.GENERAL_PRACTITIONER,
        schedules: List<Schedule> = emptyList(),
        isActive: Boolean = true,
    ) = Medication(
        id = MedicationId(id),
        name = name,
        defaultDose = defaultDose,
        usedSince = usedSince,
        useUntil = useUntil,
        prescribedBy = prescribedBy,
        schedules = schedules,
        isActive = isActive,
    )

    fun newMedication(
        name: String = "Ibuprofen",
        defaultDose: Quantity = mg40,
        usedSince: LocalDate = startDate,
        useUntil: LocalDate? = null,
        prescribedBy: Prescriber = Prescriber.GENERAL_PRACTITIONER,
        schedules: List<Schedule> = emptyList(),
    ) = NewMedication(
        name = name,
        defaultDose = defaultDose,
        usedSince = usedSince,
        useUntil = useUntil,
        prescribedBy = prescribedBy,
        schedules = schedules,
    )

    fun everyDay(vararg hours: Int, amount: Quantity = mg40) =
        Schedule.EveryNDays(amount, intervalDays = 1, times = hours.map { time(it) })
}
