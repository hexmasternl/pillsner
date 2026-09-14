package nl.hexmaster.pillsner.shared.wear

/**
 * Where the phone puts the list and where the watch looks for it (design D2, D3).
 *
 * One data item at one path, replaced whole on every change: the list is small, replacing it is
 * atomic, and a dose that was answered is simply absent from the next payload.
 */
object WearSyncContract {

    /** The Data Layer path of the upcoming-doses item. */
    const val PATH = "/pillsner/upcoming-doses"

    /** The key inside that item's data map holding the JSON payload. */
    const val KEY_JSON = "json"
}
