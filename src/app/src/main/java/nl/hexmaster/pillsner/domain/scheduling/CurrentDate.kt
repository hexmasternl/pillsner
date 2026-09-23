package nl.hexmaster.pillsner.domain.scheduling

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/** Never wait less than this between checks, so a clock sitting just before midnight cannot spin. */
private val MIN_WAIT: Duration = Duration.ofSeconds(1)

/**
 * Today's date in [clock]'s zone, emitted at once and again each time midnight passes, for anything
 * shown live that depends on the date: "expires in 30 days" turns into "expires soon" overnight
 * without any data changing (`medicine-stock-tracking`'s "Tile and details-screen heads-up is a live
 * read" decision).
 *
 * The wait is measured with the coroutine clock, which does not advance while the device sleeps,
 * so on its own this can emit late after a long sleep. Screens collect it only while visible and
 * restart it when they return, and a restart emits the date as it is then.
 */
fun currentDates(clock: Clock): Flow<LocalDate> = flow {
    while (true) {
        val today = LocalDate.now(clock)
        emit(today)
        val nextMidnight = today.plusDays(1).atStartOfDay(clock.zone).toInstant()
        delay(maxOf(Duration.between(clock.instant(), nextMidnight), MIN_WAIT).toMillis())
    }
}.distinctUntilChanged()
