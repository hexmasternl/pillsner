package nl.hexmaster.pillsner.domain.reset

/**
 * Forgets what the app observed about the doses that have just been erased.
 *
 * Today that is two things, both kept outside the database: the record that a reminder went
 * missing in silence, which the Home banner is raised by, and the stock warnings still waiting to
 * be shown (`medicine-stock-tracking`). Both are evidence about data the reset has removed, so
 * leaving them behind would show a warning about a history the user has just asked the app to
 * forget, or attach it to a medicine added afterwards that happens to reuse an id.
 *
 * A port for the same reason as [ReminderTeardown]; `AppContainer` binds it to the reminder
 * preferences and the stock warning queue.
 */
fun interface ReminderHistoryReset {
    suspend fun forget()
}
