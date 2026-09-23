package nl.hexmaster.pillsner.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import nl.hexmaster.pillsner.wear.ui.UpcomingDosesScreen
import nl.hexmaster.pillsner.wear.ui.WatchViewModel
import nl.hexmaster.pillsner.wear.ui.theme.PillsnerWearTheme

/**
 * The watch app's only activity. One screen, no navigation.
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

@Composable
fun WearApp(viewModelFactory: ViewModelProvider.Factory) {
    val viewModel: WatchViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PillsnerWearTheme {
        UpcomingDosesScreen(uiState)
    }
}
