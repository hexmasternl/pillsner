package nl.hexmaster.pillsner.data.reset

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.hexmaster.pillsner.data.db.PillsnerDatabase
import nl.hexmaster.pillsner.domain.reset.AppDataEraser

/**
 * The wired [AppDataEraser]: Room's own whole-database clear (design D2).
 *
 * `clearAllTables()` rather than a delete on each DAO, for three reasons. It is one transaction, so
 * the reset cannot half-happen. It needs no DAO to grow a delete, which keeps *Medications are never
 * removed* true of every query the app has — the exception lives here, in the one place that is
 * plainly about resetting, rather than as a method on the medication DAO that some later change
 * might reach for. And it resets the auto-increment counters, so the app starts genuinely fresh
 * rather than with ids counting on from a history that is gone.
 *
 * The schema is untouched: the version stays where it is, no migration runs, and the database is
 * still open and usable afterwards.
 */
class RoomAppDataEraser(private val database: PillsnerDatabase) : AppDataEraser {

    override suspend fun eraseAll() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }
}
