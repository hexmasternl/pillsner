package nl.hexmaster.pillsner.domain.repository

/**
 * Runs several repository calls as one all-or-nothing unit, so the domain layer can ask for a
 * transaction without knowing it is Room underneath.
 *
 * Two transactions never interleave: a second one waits until the first has committed, so a read
 * made inside [inTransaction] is still true when the writes that depend on it land. This is what
 * keeps two answers for the same medicine, given at the same moment from two surfaces, from both
 * deducting from the same remaining stock (`medicine-stock-tracking`'s "First-expiry-first-out
 * consumption" requirement).
 *
 * Nothing inside [block] should touch anything outside the database — a notification, an alarm, a
 * preferences file — since that would not roll back with it.
 */
interface TransactionRunner {
    suspend fun <R> inTransaction(block: suspend () -> R): R
}
