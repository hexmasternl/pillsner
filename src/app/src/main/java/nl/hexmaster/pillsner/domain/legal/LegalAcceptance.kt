package nl.hexmaster.pillsner.domain.legal

import java.time.Instant

/**
 * What the user accepted, and when (design D3).
 *
 * Written as one record: a version is never stored without the moment it was accepted, so the
 * Settings screen can always say when, for whatever it says the user agreed to.
 */
data class LegalAcceptance(
    val disclaimerVersion: Int,
    val termsVersion: Int,
    val acceptedAt: Instant,
)
