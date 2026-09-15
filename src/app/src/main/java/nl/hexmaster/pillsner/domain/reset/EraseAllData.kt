package nl.hexmaster.pillsner.domain.reset

/**
 * Resetting the app: erase everything, then settle what hung off it (design D3).
 *
 * The order is fixed and is the whole design of this use case. The irreversible step goes first, so
 * a failure can never leave the data half-erased with the app already behaving as though it had
 * gone. The two corrections follow, and both are self-healing if they in turn fail: a surviving
 * alarm wakes to an empty database and cancels itself, and a surviving notification answers into a
 * dose id that no longer exists, which is already a no-op.
 *
 * Nothing here can be undone, which is why the only caller is a dialog the user has ticked a
 * checkbox in.
 */
class EraseAllData(
    private val eraser: AppDataEraser,
    private val teardown: ReminderTeardown,
    private val refresh: ReminderRefresh,
) {

    suspend operator fun invoke() {
        eraser.eraseAll()
        teardown.cancelAll()
        refresh.refresh()
    }
}
