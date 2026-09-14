package nl.hexmaster.pillsner.wear

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.wearable.Wearable
import java.time.Clock
import nl.hexmaster.pillsner.wear.data.PhoneConnectivity
import nl.hexmaster.pillsner.wear.data.UpcomingDosesRepository
import nl.hexmaster.pillsner.wear.ui.WatchViewModel

/**
 * Manual dependency injection, mirroring the phone's `AppContainer`: one object graph, built once
 * by the application, no framework. Two apps, one mechanism (CLAUDE.md).
 */
class WearContainer(context: Context) {

    private val appContext = context.applicationContext
    private val clock: Clock = Clock.systemDefaultZone()

    private val upcomingDoses = UpcomingDosesRepository(Wearable.getDataClient(appContext))
    private val phoneConnectivity = PhoneConnectivity(Wearable.getNodeClient(appContext))

    val viewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            WatchViewModel(
                payloads = upcomingDoses.observe(),
                isPhoneConnected = phoneConnectivity::isPhoneConnected,
                clock = clock,
            )
        }
    }
}
