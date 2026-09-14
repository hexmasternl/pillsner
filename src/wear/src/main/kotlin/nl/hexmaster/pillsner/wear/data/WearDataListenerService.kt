package nl.hexmaster.pillsner.wear.data

import com.google.android.gms.wearable.WearableListenerService

/**
 * Exists so Play services can start this app's process when the phone publishes a new list while
 * the watch app is closed (design D4).
 *
 * It has nothing to do: receiving the callback is what lets Play services persist the item, and the
 * screen reads the persisted item when it opens. Anything more would be background work on a watch
 * battery for a list nobody is looking at.
 */
class WearDataListenerService : WearableListenerService()
