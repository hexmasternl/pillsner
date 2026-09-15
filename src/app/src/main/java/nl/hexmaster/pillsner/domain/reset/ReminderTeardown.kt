package nl.hexmaster.pillsner.domain.reset

/**
 * Takes down every reminder the app currently has on screen.
 *
 * A port rather than a direct call, so that erasing the data stays a domain operation with no
 * Android type anywhere near it. `AppContainer` binds it to the notifier.
 */
fun interface ReminderTeardown {
    fun cancelAll()
}
