package nl.hexmaster.pillsner.domain.scheduling

import java.time.Clock
import java.time.Instant

/**
 * The one-year dose-history retention cutoff (design D1, spec: dose-history-retention).
 *
 * Pure: reads [clock]'s zone fresh on every call rather than caching it anywhere, so a device
 * time-zone change between two housekeeping wakes is reflected in the very next cutoff computed,
 * never a stale one from an earlier wake. Local calendar-date arithmetic, matching the convention
 * `medicine-usage-history` already uses for its "1 month"/"3 months" periods, so month lengths,
 * leap days and daylight-saving transitions need no special case.
 */
class PurgeExpiredDoseHistory(private val clock: Clock) {

    /**
     * The moment before which a dose's history may be purged: one calendar year before
     * [trustedNow], at the start of that local day in the current system zone.
     */
    operator fun invoke(trustedNow: Instant): Instant {
        val zone = clock.zone
        // LocalDate.ofInstant(Instant, ZoneId) needs API 34; this is the minSdk-26 equivalent.
        val today = trustedNow.atZone(zone).toLocalDate()
        return today.minusYears(1).atStartOfDay(zone).toInstant()
    }
}
