package nl.hexmaster.pillsner.domain.reset

/**
 * Brings the alarms back in line with a database that has just changed underneath them.
 *
 * The same wake every other change to the medicines triggers; a reset is only the largest of them.
 * A port for the same reason as [ReminderTeardown].
 */
fun interface ReminderRefresh {
    fun refresh()
}
