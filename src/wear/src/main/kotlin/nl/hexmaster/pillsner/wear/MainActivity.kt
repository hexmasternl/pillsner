package nl.hexmaster.pillsner.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import nl.hexmaster.pillsner.wear.ui.DoseDetailsScreen
import nl.hexmaster.pillsner.wear.ui.UpcomingDosesScreen
import nl.hexmaster.pillsner.wear.ui.WatchViewModel
import nl.hexmaster.pillsner.wear.ui.theme.PillsnerWearTheme

/**
 * The watch app's only activity. Two screens: the agenda, and the read-only details of one dose
 * (`wear-day-overview` design D4).
 *
 * Enables edge-to-edge explicitly, as the phone's `MainActivity` does, so the window behaves the
 * same on every Wear OS version and Google Play stops flagging the watch bundle (issue #72,
 * edge-to-edge-insets design D1). The Wear Compose scaffolds own the round-screen and time-text
 * padding, so nothing else here changes.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as PillsnerWearApplication).container
        setContent { WearApp(container.viewModelFactory) }
    }
}

/**
 * The agenda, or the details of the dose the user tapped.
 *
 * Which one is showing is a single remembered dose id rather than a navigation graph: there is one
 * step, it is read-only, and the watch's own back gesture is all it takes to leave it. A dose that
 * disappears from the agenda — answered on the phone while its details are open — takes the user
 * back to the list rather than leaving a dose on screen that no longer exists.
 */
@Composable
fun WearApp(viewModelFactory: ViewModelProvider.Factory) {
    val viewModel: WatchViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedDoseId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = selectedDoseId?.let(uiState::entry)

    PillsnerWearTheme {
        if (selected == null) {
            UpcomingDosesScreen(uiState, onDoseClick = { selectedDoseId = it })
        } else {
            BackHandler { selectedDoseId = null }
            DoseDetailsScreen(selected, uiState.locale)
        }
    }
}
