package nl.hexmaster.pillsner.domain.reset

/**
 * Forgets what the reminders observed about the doses that have just been erased.
 *
 * Today that is one thing: the record that a reminder went missing in silence, which the Home
 * banner is raised by. It is evidence about doses the reset has removed, so leaving it behind would
 * show a warning about a history the user has just asked the app to forget.
 *
 * A port for the same reason as [ReminderTeardown]; `AppContainer` binds it to the reminder
 * preferences.
 */
fun interface ReminderHistoryReset {
    suspend fun forget()
}
