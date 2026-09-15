package nl.hexmaster.pillsner.domain.reset

/**
 * Empties everything the app has stored about medication (design D2).
 *
 * Every medicine, every schedule and the whole intake history, in one transaction, so a reset is
 * never half done. Settings are not medication data and are not touched: the language, the app lock
 * and the recorded acceptance of the legal documents all survive, because the app stays configured
 * — it is simply empty.
 *
 * This is the single exception to *Medications are never removed*. It exists because a user handing
 * on a phone, or one who filled the app with a trial run, otherwise has no way out but to uninstall.
 */
fun interface AppDataEraser {

    /** Empties every stored medicine, schedule and dose. Irreversible. */
    suspend fun eraseAll()
}
