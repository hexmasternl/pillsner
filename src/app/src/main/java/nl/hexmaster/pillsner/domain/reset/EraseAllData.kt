package nl.hexmaster.pillsner.domain.reset

/**
 * Resetting the app: erase everything, then settle what hung off it (design D3).
 *
 * The order is fixed and is the whole design of this use case. The irreversible step goes first, so
 * a failure can never leave the data half-erased with the app already behaving as though it had
 * gone. The corrections follow, and each is self-healing if it in turn fails: a surviving
 * alarm wakes to an empty database and cancels itself, a surviving notification answers into a
 * dose id that no longer exists, which is already a no-op, and a surviving missed-reminder record
 * is cleared by the next time the user acts on the banner it raises.
 *
 * Nothing here can be undone, which is why the only caller is a dialog the user has ticked a
 * checkbox in.
 */
class EraseAllData(
    private val eraser: AppDataEraser,
    private val teardown: ReminderTeardown,
    private val history: ReminderHistoryReset,
    private val refresh: ReminderRefresh,
) {

    suspend operator fun invoke() {
        eraser.eraseAll()
        teardown.cancelAll()
        history.forget()
        refresh.refresh()
    }
}
