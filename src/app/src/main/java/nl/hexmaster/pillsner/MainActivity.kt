package nl.hexmaster.pillsner

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.applock.ui.AppLockViewModel
import nl.hexmaster.pillsner.applock.ui.BiometricAuthenticator
import nl.hexmaster.pillsner.di.AppContainer
import nl.hexmaster.pillsner.ui.PillsnerApp
import nl.hexmaster.pillsner.ui.locale.AppLocale
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme

/**
 * The app's single activity. Everything visible is Compose inside [PillsnerApp], which gates its
 * own content behind the app lock (app-login design D1).
 *
 * Extends [FragmentActivity], not `ComponentActivity`, because `BiometricPrompt` in the stable
 * `androidx.biometric` 1.1.0 requires one (design D6); a `FragmentActivity` is itself a
 * `ComponentActivity`, so `enableEdgeToEdge()` and `setContent` keep working unchanged. This is
 * the only file the app-login change alters in the `app-welcome-screen` scaffold.
 */
class MainActivity : FragmentActivity() {

    private val container: AppContainer by lazy { (application as PillsnerApplication).container }

    /**
     * Everything on screen reads its strings from here, so this is where the app's own language
     * is put in front of the phone's (app-settings-language design D3).
     */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    private val appLockViewModel: AppLockViewModel by viewModels { container.viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val biometricAuthenticator = BiometricAuthenticator(this)

        // Hides app content from recents and screenshots while the lock is enabled (design D8).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appLockViewModel.uiState.collect { state ->
                    if (state.pinLockEnabled) {
                        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }

        setContent {
            PillsnerTheme {
                // Created above the lock gate inside PillsnerApp (design D1) so navigation state
                // survives a relock.
                val navController = rememberNavController()
                PillsnerApp(
                    viewModelFactory = container.viewModelFactory,
                    appLockViewModel = appLockViewModel,
                    biometricAuthenticator = biometricAuthenticator,
                    navController = navController,
                )
            }
        }
    }
}
