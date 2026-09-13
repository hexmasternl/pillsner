package nl.hexmaster.pillsner.applock.ui

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

private const val MAX_PIN_LENGTH = 6
private const val MIN_PIN_LENGTH = 4

/** Which of the two PIN setup steps is showing (spec "Enabling the lock requires ... a PIN"). */
private enum class SetupStep { Enter, Confirm }

/**
 * The PIN setup flow (design D10, task 6.1): enter, then confirm. Nothing is persisted until both
 * entries match — leaving by [onBack] or the process going to the background before that simply
 * discards this screen's local state, so no partial PIN is ever stored (spec "Setup abandoned").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(
    onBack: () -> Unit,
    onPinConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(SetupStep.Enter) }
    var firstEntry by remember { mutableStateOf("") }
    var currentEntry by remember { mutableStateOf("") }
    var mismatchShown by remember { mutableStateOf(false) }
    var tooShortShown by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (step == SetupStep.Enter) R.string.applock_setup_choose_title else R.string.applock_setup_confirm_title,
                        ),
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
                text = stringResource(
                    if (step == SetupStep.Enter) R.string.applock_setup_choose_instruction else R.string.applock_setup_confirm_instruction,
                ),
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
            } else if (tooShortShown) {
                ErrorMessage(text = stringResource(R.string.applock_setup_length_hint))
                Spacer(Modifier.height(Spacing.md))
            }

            Spacer(Modifier.weight(1f, fill = true))

            PinKeypad(
                enteredLength = currentEntry.length,
                maxLength = MAX_PIN_LENGTH,
                enabled = true,
                submitEnabled = currentEntry.length >= MIN_PIN_LENGTH,
                onDigit = { digit ->
                    if (currentEntry.length < MAX_PIN_LENGTH) {
                        mismatchShown = false
                        tooShortShown = false
                        currentEntry += digit
                    }
                },
                onBackspace = { currentEntry = currentEntry.dropLast(1) },
                onSubmit = submit@{
                    if (currentEntry.length < MIN_PIN_LENGTH) {
                        tooShortShown = true
                        return@submit
                    }
                    when (step) {
                        SetupStep.Enter -> {
                            firstEntry = currentEntry
                            currentEntry = ""
                            step = SetupStep.Confirm
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

@PreviewLightDark
@Composable
private fun PinSetupScreenPreview() {
    PillsnerTheme {
        Surface { PinSetupScreen(onBack = {}, onPinConfirmed = {}) }
    }
}

@Preview(fontScale = 2f)
@Composable
private fun PinSetupScreenLargeFontPreview() {
    PillsnerTheme {
        Surface { PinSetupScreen(onBack = {}, onPinConfirmed = {}) }
    }
}
