package nl.hexmaster.pillsner.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.RoomDoseRepository
import nl.hexmaster.pillsner.data.RoomMedicationRepository
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: medication-persistence round-trip fidelity, atomic save, cascade delete. */
@RunWith(AndroidJUnit4::class)
class MedicationDaoTest {

    private lateinit var database: PillsnerDatabase
    private lateinit var dao: MedicationDao
    private lateinit var repository: RoomMedicationRepository

    private val today: LocalDate = LocalDate.of(2026, 9, 13)
    private val mg40 = Quantity.of("40", DoseUnit.MILLIGRAM)

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PillsnerDatabase::class.java)
            .build()
        dao = database.medicationDao()
        repository = RoomMedicationRepository(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aDecimalDefaultDose_staysExact() = runBlocking {
        val id = repository.add(medication(defaultDose = Quantity.of("2.5", DoseUnit.MILLILITRE)))

        val stored = repository.observeAll().first().single()
        assertEquals(id, stored.id)
        assertEquals(Quantity.of("2.5", DoseUnit.MILLILITRE), stored.defaultDose)
        assertEquals(BigDecimal("2.5"), stored.defaultDose.value)
    }

    @Test
    fun anEveryNDaysSchedule_roundTrips() = runBlocking {
        val schedule = Schedule.EveryNDays(mg40, 2, listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))

        repository.add(medication(schedules = listOf(schedule)))

        assertEquals(schedule, repository.observeAll().first().single().schedules.single())
    }

    @Test
    fun aWeekdaysSchedule_roundTrips() = runBlocking {
        val schedule = Schedule.OnWeekdays(
            mg40,
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            listOf(LocalTime.of(8, 0)),
        )

        repository.add(medication(schedules = listOf(schedule)))

        assertEquals(schedule, repository.observeAll().first().single().schedules.single())
    }

    @Test
    fun anEveryNHoursSchedule_roundTrips() = runBlocking {
        val schedule = Schedule.EveryNHours(mg40, 12, LocalTime.of(8, 0))

        repository.add(medication(schedules = listOf(schedule)))

        assertEquals(schedule, repository.observeAll().first().single().schedules.single())
    }

    @Test
    fun scheduleOrder_isPreserved() = runBlocking {
        val schedules = listOf(
            Schedule.EveryNHours(mg40, 12, LocalTime.of(8, 0)),
            Schedule.EveryNDays(Quantity.of("20", DoseUnit.MILLIGRAM), 1, listOf(LocalTime.of(9, 0))),
            Schedule.OnWeekdays(
                Quantity.of("10", DoseUnit.MILLIGRAM),
                setOf(DayOfWeek.SUNDAY),
                listOf(LocalTime.of(10, 0)),
            ),
        )

        repository.add(medication(schedules = schedules))

        assertEquals(schedules, repository.observeAll().first().single().schedules)
    }

    @Test
    fun anAbsentUseUntil_staysAbsent() = runBlocking {
        repository.add(medication(useUntil = null))

        assertNull(repository.observeAll().first().single().useUntil)
    }

    @Test
    fun everyFieldOfAMedication_roundTrips() = runBlocking {
        repository.add(
            medication(
                name = "Metoprolol",
                useUntil = today.plusDays(30),
                prescribedBy = Prescriber.SPECIALIST,
            ),
        )

        val stored = repository.observeAll().first().single()
        assertEquals("Metoprolol", stored.name)
        assertEquals(today, stored.usedSince)
        assertEquals(today.plusDays(30), stored.useUntil)
        assertEquals(Prescriber.SPECIALIST, stored.prescribedBy)
        assertTrue(stored.isActive)
    }

    @Test
    fun setActive_flipsTheFlagAndLeavesSchedulesAlone() = runBlocking {
        val schedules = listOf(
            Schedule.EveryNDays(mg40, 2, listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))),
            Schedule.EveryNHours(mg40, 12, LocalTime.of(7, 0)),
        )
        val id = repository.add(medication(schedules = schedules))

        repository.setActive(id, isActive = false)

        val stored = repository.observeAll().first().single()
        assertEquals(false, stored.isActive)
        assertEquals(schedules, stored.schedules)
        assertEquals("Ibuprofen", stored.name)
    }

    @Test
    fun setActive_leavesDoseRowsAlone() = runBlocking {
        val id = repository.add(medication())
        val doses = RoomDoseRepository(database.doseDao())
        doses.insertPlanned(
            listOf(
                nl.hexmaster.pillsner.domain.model.PlannedDose(
                    medicationId = id,
                    medicationName = "Ibuprofen",
                    amount = mg40,
                    scheduledAt = java.time.Instant.parse("2026-09-14T06:00:00Z"),
                ),
            ),
        )

        repository.setActive(id, isActive = false)

        assertEquals(1, doses.pending().size)
    }

    @Test
    fun setActive_onAnUnknownMedicineDoesNothing() = runBlocking {
        repository.add(medication())

        repository.setActive(MedicationId(404), isActive = false)

        assertTrue(repository.observeAll().first().single().isActive)
    }

    @Test
    fun setActive_reEmitsToACollector() = runBlocking {
        val id = repository.add(medication())
        assertTrue(repository.observeAll().first().single().isActive)

        repository.setActive(id, isActive = false)

        assertEquals(false, repository.observeAll().first().single().isActive)
    }

    @Test
    fun update_replacesEveryFieldUnderTheSameIdentifier() = runBlocking {
        val id = repository.add(medication(name = "Metoprolol"))
        val stored = repository.observeAll().first().single()

        repository.update(
            stored.copy(
                name = "Metoprolol retard",
                defaultDose = Quantity.of("20", DoseUnit.MILLIGRAM),
                useUntil = today.plusDays(30),
                prescribedBy = Prescriber.PHARMACIST,
                isActive = false,
            ),
        )

        val updated = repository.observeAll().first().single()
        assertEquals(id, updated.id)
        assertEquals("Metoprolol retard", updated.name)
        assertEquals(Quantity.of("20", DoseUnit.MILLIGRAM), updated.defaultDose)
        assertEquals(today.plusDays(30), updated.useUntil)
        assertEquals(Prescriber.PHARMACIST, updated.prescribedBy)
        assertEquals(false, updated.isActive)
    }

    @Test
    fun update_replacesTheSchedulesInOrderWithNoLeftovers() = runBlocking {
        val before = listOf(
            Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(8, 0))),
            Schedule.EveryNHours(mg40, 12, LocalTime.of(9, 0)),
        )
        repository.add(medication(schedules = before))
        val stored = repository.observeAll().first().single()
        val after = listOf(
            Schedule.EveryNHours(Quantity.of("10", DoseUnit.MILLIGRAM), 8, LocalTime.of(7, 0)),
            Schedule.OnWeekdays(mg40, setOf(DayOfWeek.MONDAY), listOf(LocalTime.of(10, 0))),
            Schedule.EveryNDays(mg40, 3, listOf(LocalTime.of(11, 0))),
        )

        repository.update(stored.copy(schedules = after))

        assertEquals(after, repository.observeAll().first().single().schedules)
    }

    @Test
    fun update_canClearEverySchedule() = runBlocking {
        repository.add(medication(schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(8, 0))))))
        val stored = repository.observeAll().first().single()

        repository.update(stored.copy(schedules = emptyList()))

        val updated = repository.observeAll().first().single()
        assertEquals(emptyList<Schedule>(), updated.schedules)
        assertEquals("Ibuprofen", updated.name)
    }

    @Test
    fun update_leavesDoseRowsAlone() = runBlocking {
        val id = repository.add(medication())
        val doses = RoomDoseRepository(database.doseDao())
        doses.insertPlanned(
            listOf(
                nl.hexmaster.pillsner.domain.model.PlannedDose(
                    medicationId = id,
                    medicationName = "Ibuprofen",
                    amount = mg40,
                    scheduledAt = java.time.Instant.parse("2026-09-14T06:00:00Z"),
                ),
            ),
        )
        val before = doses.pending().single()
        val stored = repository.observeAll().first().single()

        repository.update(stored.copy(name = "Ibuprofen retard", defaultDose = Quantity.of("1", DoseUnit.TABLET)))

        // The dose keeps the snapshot it was planned with: history is a fact, not a view.
        assertEquals(before, doses.pending().single())
    }

    @Test
    fun update_onAnUnknownMedicineFailsAndWritesNothing() = runBlocking {
        repository.add(medication())
        val stored = repository.observeAll().first().single()

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                repository.update(
                    stored.copy(
                        id = MedicationId(404),
                        schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(8, 0)))),
                    ),
                )
            }
        }

        assertEquals(stored, repository.observeAll().first().single())
    }

    @Test
    fun get_returnsTheMedicineWithItsSchedules() = runBlocking {
        val schedules = listOf(Schedule.EveryNHours(mg40, 12, LocalTime.of(8, 0)))
        val id = repository.add(medication(schedules = schedules))

        val found = checkNotNull(repository.get(id))

        assertEquals("Ibuprofen", found.name)
        assertEquals(schedules, found.schedules)
    }

    @Test
    fun get_onAnUnknownMedicineReturnsNothing() = runBlocking {
        assertNull(repository.get(MedicationId(404)))
    }

    @Test
    fun theDatabaseIsStillAtItsCurrentVersion() {
        assertEquals(PillsnerDatabase.VERSION, database.openHelper.readableDatabase.version)
    }

    @Test
    fun theSchemaStillCascades_evenThoughNothingEverDeletesAMedicine() = runBlocking {
        // Production code has no way to delete a medication, and must not gain one: a medicine is
        // deactivated, never removed. The cascade is still part of the schema, so it is exercised
        // here through raw SQL on the test database rather than through a DAO method.
        val id = repository.add(
            medication(schedules = listOf(Schedule.EveryNHours(mg40, 12, LocalTime.of(8, 0)))),
        )

        database.openHelper.writableDatabase.execSQL("DELETE FROM medications WHERE id = ${id.value}")

        assertEquals(emptyList<Any>(), repository.observeAll().first())
        assertNull(dao.getWithSchedules(id.value))
    }

    @Test
    fun aFailingScheduleInsert_leavesNoMedicationRow() = runBlocking {
        val medication = MedicationEntity(
            name = "Metoprolol",
            defaultDoseValue = BigDecimal("40"),
            defaultDoseUnit = DoseUnit.MILLIGRAM.name,
            usedSince = today,
            useUntil = null,
            prescribedBy = Prescriber.SELF.name,
            isActive = true,
        )
        // Two schedule rows claiming the same primary key: the second insert violates it.
        val clashing = List(2) {
            ScheduleEntity(
                id = 5L,
                medicationId = 0L,
                position = it,
                kind = ScheduleKind.EVERY_N_HOURS,
                amountValue = BigDecimal("40"),
                amountUnit = DoseUnit.MILLIGRAM.name,
                intervalHours = 12,
                firstDoseAt = LocalTime.of(8, 0),
            )
        }

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { dao.insert(medication, clashing) }
        }

        assertEquals(emptyList<Any>(), repository.observeAll().first())
    }

    @Test
    fun theStream_reEmitsWhenAMedicationIsAdded() = runBlocking {
        assertEquals(emptyList<Any>(), repository.observeAll().first())

        repository.add(medication(name = "Ibuprofen"))

        assertEquals(listOf("Ibuprofen"), repository.observeAll().first().map { it.name })
    }

    @Test
    fun aScheduleRowMissingItsTimes_failsWithADescriptiveError() {
        val row = ScheduleEntity(
            id = 7L,
            medicationId = 1L,
            position = 0,
            kind = ScheduleKind.EVERY_N_DAYS,
            amountValue = BigDecimal("40"),
            amountUnit = DoseUnit.MILLIGRAM.name,
            intervalDays = 1,
            times = null,
        )

        val error = assertThrows(IllegalStateException::class.java) { row.toDomain() }

        assertTrue(error.message.orEmpty().contains("7"))
        assertTrue(error.message.orEmpty().contains(ScheduleKind.EVERY_N_DAYS))
        assertTrue(error.message.orEmpty().contains("times"))
    }

    private fun medication(
        name: String = "Ibuprofen",
        defaultDose: Quantity = mg40,
        useUntil: LocalDate? = null,
        prescribedBy: Prescriber = Prescriber.GENERAL_PRACTITIONER,
        schedules: List<Schedule> = emptyList(),
    ) = NewMedication(
        name = name,
        defaultDose = defaultDose,
        usedSince = today,
        useUntil = useUntil,
        prescribedBy = prescribedBy,
        schedules = schedules,
    )
}
