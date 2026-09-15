package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import android.os.PowerManager

/**
 * Whether the platform is letting Pillsner run freely enough to deliver a reminder (design D6).
 *
 * Battery optimisation is the third thing that can silently stop a reminder, alongside
 * notifications being denied and exact alarms being withheld. It is the only one of the three the
 * app cannot detect from a failure, so it is read directly and reported on the Home banner.
 *
 * A function interface so tests can state the answer without a `PowerManager`.
 */
fun interface BatteryOptimisationState {

    /** True when the user has taken Pillsner out of battery optimisation. */
    fun isExempt(): Boolean
}

/** The wired [BatteryOptimisationState]: what the platform's own power manager says. */
class AndroidBatteryOptimisationState(context: Context) : BatteryOptimisationState {

    private val appContext = context.applicationContext
    private val powerManager = appContext.getSystemService(PowerManager::class.java)

    override fun isExempt(): Boolean =
        powerManager.isIgnoringBatteryOptimizations(appContext.packageName)
}
