package nl.hexmaster.pillsner.wear.ui

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.pillsner.shared.wear.SyncedDose
import nl.hexmaster.pillsner.shared.wear.SyncedDoses
import nl.hexmaster.pillsner.shared.wear.SyncedMedicineDetails
import nl.hexmaster.pillsner.wear.domain.AgendaDay
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Spec: what the watch screen shows, and what it says when it cannot be sure. */
@OptIn(ExperimentalCoroutinesApi::class)
class WatchViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val now = Instant.parse("2026-09-13T10:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneId.of("Europe/Amsterdam"))
    private val payloads = MutableStateFlow<SyncedDoses?>(null)
    private var phoneConnected = true

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.collecting(): WatchViewModel {
        val viewModel = WatchViewModel(payloads, { phoneConnected }, clock)
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun doseAt(id: Long, offset: Duration, name: String = "Medicine $id") =
        SyncedDose(id, name, "1 tablet", now.plus(offset).toEpochMilli())

    private fun payload(vararg doses: SyncedDose, languageTag: String = "en") = SyncedDoses(
        languageTag = languageTag,
        publishedAtEpochMillis = now.toEpochMilli(),
        doses = doses.toList(),
    )

    /** A clock whose instant can be moved forward from the test body, in lockstep with virtual time. */
    private class ControllableClock(var instant: Instant, private val zone: ZoneId) : Clock() {
        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = ControllableClock(instant, zone)

        override fun instant(): Instant = instant
    }

    @Test
    fun `before anything has ever arrived the screen knows it has nothing`() = runTest(dispatcher) {
        val viewModel = collecting()

        val state = viewModel.uiState.value
        assertFalse(state.hasData)
        assertTrue(state.isEmpty)
    }

    @Test
    fun `today and tomorrow become the agenda, each under its own heading`() = runTest(dispatcher) {
        // Noon in Amsterdam, so nine hours on is still tonight and a day on is tomorrow.
        payloads.value = payload(
            doseAt(1, Duration.ofHours(1), "Ibuprofen"),
            doseAt(2, Duration.ofHours(9), "Metformin"),
            doseAt(3, Duration.ofHours(25), "Simvastatin"),
        )

        val state = collecting().uiState.value

        assertEquals(listOf(AgendaDay.TODAY, AgendaDay.TOMORROW), state.sections.map { it.day })
        assertEquals(listOf("Ibuprofen", "Metformin", "Simvastatin"), state.entries.map { it.name })
        assertTrue(state.hasData)
    }

    @Test
    fun `doses due at the same time share one group`() = runTest(dispatcher) {
        payloads.value = payload(
            doseAt(1, Duration.ofHours(1), "Ibuprofen"),
            doseAt(2, Duration.ofHours(1), "Metformin"),
            doseAt(3, Duration.ofHours(3), "Simvastatin"),
        )

        val groups = collecting().uiState.value.sections.single().groups

        assertEquals(listOf(2, 1), groups.map { it.doses.size })
    }

    @Test
    fun `the medicine behind a dose travels with it, for the details screen`() = runTest(dispatcher) {
        payloads.value = payload(
            doseAt(1, Duration.ofHours(1), "Ibuprofen").copy(
                details = SyncedMedicineDetails(
                    defaultDoseText = "400 mg",
                    scheduleLines = listOf("400 mg twice a day"),
                    stockText = "24 tablets",
                ),
            ),
        )

        val entry = collecting().uiState.value.entries.single()

        assertEquals("400 mg", entry.defaultDoseText)
        assertEquals(listOf("400 mg twice a day"), entry.scheduleLines)
        assertEquals("24 tablets", entry.stockText)
        assertTrue(entry.hasDetails)
    }

    @Test
    fun `a dose from a phone that sends no details is shown without them`() = runTest(dispatcher) {
        payloads.value = payload(doseAt(1, Duration.ofHours(1), "Ibuprofen"))

        val state = collecting().uiState.value
        val entry = state.entries.single()

        assertFalse(entry.hasDetails)
        assertEquals(entry, state.entry(entry.doseId))
    }

    @Test
    fun `a dose whose time has gone is marked overdue and comes first`() = runTest(dispatcher) {
        payloads.value = payload(
            doseAt(1, Duration.ofHours(2), "Later"),
            doseAt(2, Duration.ofMinutes(-15), "Earlier"),
        )

        val entries = collecting().uiState.value.entries

        assertEquals(listOf("Earlier", "Later"), entries.map { it.name })
        assertTrue(entries.first().isOverdue)
        assertFalse(entries.last().isOverdue)
    }

    @Test
    fun `a dose on the next day is flagged as tomorrow`() = runTest(dispatcher) {
        // 20:00 UTC is 22:00 in Amsterdam, so an hour on is still tonight and three hours on is
        // 01:00 the next day.
        val lateEvening = Instant.parse("2026-09-13T20:00:00Z")
        val eveningClock = Clock.fixed(lateEvening, ZoneId.of("Europe/Amsterdam"))
        payloads.value = SyncedDoses(
            languageTag = "en",
            publishedAtEpochMillis = lateEvening.toEpochMilli(),
            doses = listOf(
                SyncedDose(1, "Tonight", "1 tablet", lateEvening.plus(Duration.ofHours(1)).toEpochMilli()),
                SyncedDose(2, "After midnight", "1 tablet", lateEvening.plus(Duration.ofHours(3)).toEpochMilli()),
            ),
        )
        val viewModel = WatchViewModel(payloads, { phoneConnected }, eveningClock)
        backgroundScope.launch { viewModel.uiState.collect {} }

        val entries = viewModel.uiState.value.entries

        assertEquals(listOf("Tonight", "After midnight"), entries.map { it.name })
        assertFalse(entries.first().isTomorrow)
        assertTrue(entries.last().isTomorrow)
    }

    @Test
    fun `two empty days are an empty screen, not a missing one`() = runTest(dispatcher) {
        payloads.value = payload(doseAt(1, Duration.ofHours(60)))

        val state = collecting().uiState.value

        assertTrue(state.isEmpty)
        assertTrue("The payload did arrive; there is simply nothing due", state.hasData)
    }

    @Test
    fun `the watch reads in the language the phone is in`() = runTest(dispatcher) {
        payloads.value = payload(doseAt(1, Duration.ofHours(1)), languageTag = "nl-NL")

        assertEquals(Locale.forLanguageTag("nl-NL"), collecting().uiState.value.locale)
    }

    @Test
    fun `a payload from a later version is treated as no payload at all`() = runTest(dispatcher) {
        val raw = SyncedDoses.encode(payload(doseAt(1, Duration.ofHours(1))).copy(version = 99))
        payloads.value = SyncedDoses.decode(raw)

        val state = collecting().uiState.value

        assertFalse("Better the sync footer than a list we cannot read", state.hasData)
        assertTrue(state.isEmpty)
    }

    @Test
    fun `a phone out of reach is reported`() = runTest(dispatcher) {
        phoneConnected = false
        payloads.value = payload(doseAt(1, Duration.ofHours(1)))

        val state = collecting().uiState.value

        assertFalse(state.phoneConnected)
        assertEquals(1, state.entries.size)
    }

    @Test
    fun `a single per-minute tick checks connectivity exactly once`() = runTest(dispatcher) {
        var connectivityChecks = 0
        val viewModel = WatchViewModel(payloads, { connectivityChecks++; phoneConnected }, clock)
        backgroundScope.launch { viewModel.uiState.collect {} }

        assertEquals(1, connectivityChecks)
    }

    @Test
    fun `a payload update does not recheck connectivity, but the next minute tick does`() = runTest(dispatcher) {
        val tickingClock = ControllableClock(now, ZoneId.of("Europe/Amsterdam"))
        var connectivityChecks = 0
        payloads.value = payload(doseAt(1, Duration.ofHours(1), "Ibuprofen"))
        val viewModel = WatchViewModel(payloads, { connectivityChecks++; phoneConnected }, tickingClock)
        backgroundScope.launch { viewModel.uiState.collect {} }

        assertEquals("subscribing checks connectivity once", 1, connectivityChecks)

        payloads.value = payload(doseAt(2, Duration.ofHours(1), "Metformin"))

        assertEquals("a payload replacement alone must not recheck connectivity", 1, connectivityChecks)
        assertEquals(listOf("Metformin"), viewModel.uiState.value.entries.map { it.name })

        tickingClock.instant = now.plus(Duration.ofMinutes(1))
        advanceTimeBy(Duration.ofMinutes(1).toMillis())
        runCurrent()

        assertEquals("one minute passing checks connectivity exactly once more", 2, connectivityChecks)
    }

    @Test
    fun `a new payload replaces the list wholesale`() = runTest(dispatcher) {
        payloads.value = payload(doseAt(1, Duration.ofHours(1), "Taken in a moment"))
        val viewModel = collecting()
        assertEquals(1, viewModel.uiState.value.entries.size)

        // The dose was answered on the phone, so it is simply absent from the next payload.
        payloads.value = payload()

        assertTrue(viewModel.uiState.value.isEmpty)
    }
}
