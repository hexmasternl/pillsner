package nl.hexmaster.pillsner.ui.medicines

import android.graphics.Bitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.ui.AppLockViewModel
import nl.hexmaster.pillsner.applock.ui.BiometricAuthenticator
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.di.AppContainer
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.LabelScanResult
import nl.hexmaster.pillsner.ui.PillsnerApp
import nl.hexmaster.pillsner.ui.medicines.form.MedicationFormTestTags
import nl.hexmaster.pillsner.ui.medicines.form.QuantityFieldTestTags
import nl.hexmaster.pillsner.ui.navigation.NavigationTestTags
import nl.hexmaster.pillsner.ui.navigation.toMedicationFormRoute
import nl.hexmaster.pillsner.ui.settings.legal.LegalAcceptanceFixture
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Spec: medicine-label-scan, "A recognized photo opens a new Add medicine form prefilled from it"
 * and "Nothing usable is recognized".
 *
 * Driving the real system camera or photo picker to completion is not something an instrumented
 * Compose test can do, so these tests exercise the seam this app controls: given a
 * [LabelScanResult] (produced here by a fake `recognizeLabel`, exactly as `MedicinesViewModel.
 * scanLabel` would return it), does navigating with the route it maps to land on a correctly
 * prefilled, still-editable form.
 */
@RunWith(AndroidJUnit4::class)
class LabelScanFormPrefillTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<FragmentActivity>()

    private lateinit var navController: TestNavHostController

    private fun launchApp(fakeResult: LabelScanResult) {
        val repository = InMemoryMedicationRepository()
        val container = AppContainer(
            composeRule.activity,
            medicationRepository = repository,
            recognizeLabel = { _, _ -> fakeResult },
        )
        val appLockViewModel =
            container.viewModelFactory.create(AppLockViewModel::class.java, CreationExtras.Empty)
        container.resolveLockState()
        LegalAcceptanceFixture.accept(container)
        val biometricAuthenticator = BiometricAuthenticator(composeRule.activity)
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            PillsnerTheme {
                PillsnerApp(
                    viewModelFactory = container.viewModelFactory,
                    appLockViewModel = appLockViewModel,
                    biometricAuthenticator = biometricAuthenticator,
                    appInfo = container.appInfo,
                    navController = navController,
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            appLockViewModel.uiState.value.lockState != LockState.Loading
        }
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).also {
            it.assertIsDisplayed()
        }
    }

    /** Recognizing text from a 1x1 bitmap through the fake wired above; the pixels never matter. */
    private fun scanWith(result: LabelScanResult): LabelScanResult = runBlocking {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        // MedicinesViewModel.scanLabel is exactly what production calls; going through it (rather
        // than using `result` directly) proves the fake recognizer is really what is wired in. The
        // repository is in memory purely so this throwaway container never opens the real database.
        val container = AppContainer(
            composeRule.activity,
            medicationRepository = InMemoryMedicationRepository(),
            recognizeLabel = { _, _ -> result },
        )
        val viewModel = container.viewModelFactory.create(MedicinesViewModel::class.java, CreationExtras.Empty)
        viewModel.scanLabel(bitmap, rotationDegrees = 0)
    }

    @Test
    fun nameAndDoseRecognized_opensTheFormPrefilledAndStillEditable() {
        val recognized = scanWith(LabelScanResult("Amoxicillin", "500", DoseUnit.MILLIGRAM))
        launchApp(recognized)

        composeRule.runOnUiThread { navController.navigate(recognized.toMedicationFormRoute()) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MedicationFormTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("Amoxicillin").assertIsDisplayed()
        composeRule.onNodeWithText("500").assertIsDisplayed()
        composeRule.onNodeWithText("mg").assertIsDisplayed()

        // Still an ordinary, editable draft (spec: "every prefilled field remains ordinary").
        composeRule.onNodeWithTag(MedicationFormTestTags.NAME).performTextInput(" Retard")
        composeRule.onNodeWithText("Amoxicillin Retard").assertIsDisplayed()
    }

    @Test
    fun nameOnlyRecognized_prefillsNameAndLeavesDoseAtItsEmptyDefault() {
        val recognized = scanWith(LabelScanResult(name = "Ibuprofen"))
        launchApp(recognized)

        composeRule.runOnUiThread { navController.navigate(recognized.toMedicationFormRoute()) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Ibuprofen").assertIsDisplayed()
        composeRule.onNodeWithTag(
            MedicationFormTestTags.DOSE_PREFIX + QuantityFieldTestTags.AMOUNT,
        ).assertIsDisplayed()
    }

    @Test
    fun nothingRecognized_opensTheFormAtItsNormalDefaultsWithTheNotRecognizedMessage() {
        val recognized = scanWith(LabelScanResult())
        launchApp(recognized)

        composeRule.runOnUiThread { navController.navigate(recognized.toMedicationFormRoute()) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MedicationFormTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.NAME).assertIsDisplayed()
        composeRule.onNodeWithText("Could not read the photo. Enter the details instead.")
            .assertIsDisplayed()
    }
}
