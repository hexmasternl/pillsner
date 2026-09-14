package nl.hexmaster.pillsner.wear

import android.app.Application

/** Builds the one object graph the watch app has. */
class PillsnerWearApplication : Application() {

    lateinit var container: WearContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = WearContainer(this)
    }
}
