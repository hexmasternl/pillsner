package nl.hexmaster.pillsner.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import nl.hexmaster.pillsner.shared.wear.SyncedDose
import nl.hexmaster.pillsner.shared.wear.SyncedDoses
import nl.hexmaster.pillsner.wear.domain.UpcomingWindowFilter

/**
 * Turns the last list the phone sent into what the screen shows (design D5).
 *
 * Three things move: the payload, the minute, and whether a phone is in reach. The minute is why
 * this has a ticker at all — a dose enters the six-hour window, or an empty screen fills, with no
 * new data arriving at all.
 */
class WatchViewModel(
    payloads: Flow<SyncedDoses?>,
    isPhoneConnected: suspend () -> Boolean,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val minuteTicker = flow {
        while (true) {
            val now = Instant.now(clock)
            emit(Tick(now, isPhoneConnected()))
            delay(millisUntilNextMinute(now))
        }
    }

    val uiState: StateFlow<WatchUiState> = combine(
        payloads,
        minuteTicker,
    ) { payload, tick ->
        WatchUiState(
            entries = payload?.let { entriesFor(it, tick.now) }.orEmpty(),
            phoneConnected = tick.phoneConnected,
            hasData = payload != null,
            locale = payload?.languageTag?.let(Locale::forLanguageTag) ?: Locale.getDefault(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WatchUiState())

    private fun entriesFor(payload: SyncedDoses, now: Instant): List<WatchDoseEntry> {
        val zone = clock.zone
        val today = now.atZone(zone).toLocalDate()
        return UpcomingWindowFilter.filter(payload.doses, now).map { dose -> dose.toEntry(now, zone, today) }
    }

    private fun SyncedDose.toEntry(now: Instant, zone: ZoneId, today: java.time.LocalDate) =
        Instant.ofEpochMilli(scheduledAtEpochMillis).let { scheduledAt ->
            WatchDoseEntry(
                doseId = doseId,
                name = medicationName,
                amountText = amountText,
                scheduledAt = scheduledAt,
                isOverdue = scheduledAt.isBefore(now),
                isTomorrow = scheduledAt.atZone(zone).toLocalDate().isAfter(today),
            )
        }

    private fun millisUntilNextMinute(now: Instant): Long =
        now.until(now.truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.MINUTES), ChronoUnit.MILLIS)
            .coerceAtLeast(1)

    private data class Tick(val now: Instant, val phoneConnected: Boolean)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
