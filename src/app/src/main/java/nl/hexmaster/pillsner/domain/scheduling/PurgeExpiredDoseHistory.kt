package nl.hexmaster.pillsner.domain.scheduling

import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import nl.hexmaster.pillsner.domain.repository.DoseRepository

/**
 * Deletes dose history whose scheduled moment is more than one local calendar year in the past
 * (dose-history-retention design D2, D4).
 *
 * Only ever calls [DoseRepository.deleteHistoryBefore], so `Medication` and `Schedule` rows are
 * never touched, and a pending dose — which the rolling planning window never lets grow anywhere
 * near this old — is never affected in practice.
 *
 * Idempotent: running it again once nothing is eligible deletes nothing, which is what lets it run
 * on every wake without any "have I already purged today" bookkeeping.
 */
class PurgeExpiredDoseHistory(
    private val doseRepository: DoseRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    /**
     * @param trustedNow the trusted-now instant (see [TrustedNow]) the retention cutoff is
     *   computed from — never the raw wall clock, so a device clock set forward cannot make this
     *   purge run early.
     * @return how many dose rows were deleted.
     */
    suspend operator fun invoke(trustedNow: Instant): Int = doseRepository.deleteHistoryBefore(cutoff(trustedNow))

    /**
     * One local calendar year before [trustedNow]'s local date, at the start of that local day
     * (design D2).
     *
     * `LocalDate.minusYears` already resolves 29 February onto 28 February when the year one back
     * is not a leap year, so no bespoke leap-day handling is needed here, and a plain calendar-date
     * subtraction is unaffected by any daylight-saving transition the year in between crosses.
     */
    private fun cutoff(trustedNow: Instant): Instant {
        val zone = clock.zone
        // `LocalDate.ofInstant` needs API 34; `Instant.atZone(...).toLocalDate()` is the same
        // conversion, available since java.time's introduction at API 26 (this app's minimum).
        val today = trustedNow.atZone(zone).toLocalDate()
        val cutoffDate = today.minusYears(1)
        return ZonedDateTime.of(cutoffDate, LocalTime.MIN, zone).toInstant()
    }
}
