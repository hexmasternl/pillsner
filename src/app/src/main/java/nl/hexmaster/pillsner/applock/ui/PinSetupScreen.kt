package nl.hexmaster.pillsner.applock.ui

import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.applock.domain.Pin
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Which of the two PIN setup steps is showing (spec "Enabling the lock requires ... a PIN"). */
private enum class SetupStep { Enter, Confirm }

/**
 * Why the PIN flow was opened (design D2): to turn the lock on, or to replace the PIN it already
 * has. Both run the same two steps; only the wording and what happens at the end differ.
 */
@Keep
enum class PinSetupMode { SET_UP, CHANGE }

/**
 * The PIN setup flow (app-login design D10): enter, then confirm. Nothing is persisted until both
 * entries match — leaving by [onBack] or the process going to the background before that simply
 * discards this screen's local state, so no partial PIN is ever stored, and in [PinSetupMode.CHANGE]
 * the PIN in force stays the current one (spec "Setup abandoned", "Change abandoned").
 *
 * @param isPinInUse in [PinSetupMode.CHANGE], whether a PIN is the one already in force. Asked at
 * the first step, so the user learns before typing it twice (spec "New PIN equals current PIN").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(
    onBack: () -> Unit,
    onPinConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier,
    mode: PinSetupMode = PinSetupMode.SET_UP,
    isPinInUse: suspend (String) -> Boolean = { false },
) {
    var step by remember { mutableStateOf(SetupStep.Enter) }
    var firstEntry by remember { mutableStateOf("") }
    var currentEntry by remember { mutableStateOf("") }
    var mismatchShown by remember { mutableStateOf(false) }
    var tooShortShown by remember { mutableStateOf(false) }
    var sameAsCurrentShown by remember { mutableStateOf(false) }
    val changing = mode == PinSetupMode.CHANGE
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(titleFor(step, changing)),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = Spacing.screenEdge)
                .padding(top = Spacing.xl),
        ) {
            Text(
                text = stringResource(instructionFor(step, changing)),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.applock_setup_length_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.xl))

            if (mismatchShown) {
                ErrorMessage(text = stringResource(R.string.applock_setup_mismatch_message))
                Spacer(Modifier.height(Spacing.md))
            } else if (sameAsCurrentShown) {
                ErrorMessage(text = stringResource(R.string.applock_change_same_pin_message))
                Spacer(Modifier.height(Spacing.md))
            } else if (tooShortShown) {
                ErrorMessage(text = stringResource(R.string.applock_setup_length_hint))
                Spacer(Modifier.height(Spacing.md))
            }

            Spacer(Modifier.weight(1f, fill = true))

            PinKeypad(
                enteredLength = currentEntry.length,
                enabled = true,
                submitEnabled = currentEntry.length >= Pin.MIN_LENGTH,
                onDigit = { digit ->
                    if (currentEntry.length < Pin.MAX_LENGTH) {
                        mismatchShown = false
                        tooShortShown = false
                        sameAsCurrentShown = false
                        currentEntry += digit
                    }
                },
                onBackspace = { currentEntry = currentEntry.dropLast(1) },
                onSubmit = submit@{
                    if (currentEntry.length < Pin.MIN_LENGTH) {
                        tooShortShown = true
                        return@submit
                    }
                    when (step) {
                        SetupStep.Enter -> scope.launch {
                            val entry = currentEntry
                            if (changing && isPinInUse(entry)) {
                                sameAsCurrentShown = true
                                currentEntry = ""
                            } else {
                                firstEntry = entry
                                currentEntry = ""
                                step = SetupStep.Confirm
                            }
                        }

                        SetupStep.Confirm -> {
                            if (currentEntry == firstEntry) {
                                onPinConfirmed(currentEntry)
                            } else {
                                mismatchShown = true
                                firstEntry = ""
                                currentEntry = ""
                                step = SetupStep.Enter
                            }
                        }
                    }
                },
            )
        }
    }
}

@StringRes
private fun titleFor(step: SetupStep, changing: Boolean): Int = when {
    step == SetupStep.Enter && changing -> R.string.applock_change_choose_title
    step == SetupStep.Enter -> R.string.applock_setup_choose_title
    changing -> R.string.applock_change_confirm_title
    else -> R.string.applock_setup_confirm_title
}

@StringRes
private fun instructionFor(step: SetupStep, changing: Boolean): Int = when {
    step == SetupStep.Enter && changing -> R.string.applock_change_choose_instruction
    step == SetupStep.Enter -> R.string.applock_setup_choose_instruction
    changing -> R.string.applock_change_confirm_instruction
    else -> R.string.applock_setup_confirm_instruction
}

@PreviewLightDark
@Composable
private fun PinSetupScreenPreview() {
    PillsnerTheme {
        Surface { PinSetupScreen(onBack = {}, onPinConfirmed = {}) }
    }
}

@PreviewLightDark
@Composable
private fun PinChangeScreenPreview() {
    PillsnerTheme {
        Surface { PinSetupScreen(onBack = {}, onPinConfirmed = {}, mode = PinSetupMode.CHANGE) }
    }
}

@Preview(fontScale = 2f)
@Composable
private fun PinSetupScreenLargeFontPreview() {
    PillsnerTheme {
        Surface { PinSetupScreen(onBack = {}, onPinConfirmed = {}) }
    }
}
