package nl.hexmaster.pillsner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.fragment.app.FragmentActivity
import nl.hexmaster.pillsner.applock.ui.AppLockViewModel
import nl.hexmaster.pillsner.applock.ui.BiometricAuthenticator
import nl.hexmaster.pillsner.data.PreviewUpcomingDosesRepository
import nl.hexmaster.pillsner.di.AppContainer
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme

/**
 * Debug-only: the full shell with three sample doses, in a phone window and in a medium-width
 * window (rail). Renders content directly (unlocked), the same as the real app when the app lock
 * is disabled; the lock screen itself is previewed separately in `UnlockScreen`.
 */
@PreviewLightDark
@Preview(name = "Medium width (rail)", widthDp = 840, heightDp = 600)
@Composable
private fun PillsnerAppPreview() {
    val context = LocalContext.current
    val container = remember { AppContainer(context = context, upcomingDosesRepository = PreviewUpcomingDosesRepository()) }
    val appLockViewModel = remember { container.viewModelFactory.create(AppLockViewModel::class.java) }
    val biometricAuthenticator = remember { BiometricAuthenticator(context as FragmentActivity) }
    PillsnerTheme {
        PillsnerApp(
            viewModelFactory = container.viewModelFactory,
            appLockViewModel = appLockViewModel,
            biometricAuthenticator = biometricAuthenticator,
        )
    }
}
