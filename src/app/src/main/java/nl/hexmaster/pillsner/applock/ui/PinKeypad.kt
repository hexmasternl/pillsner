package nl.hexmaster.pillsner.applock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing
import nl.hexmaster.pillsner.ui.theme.TabularNumbers

/**
 * A numeric keypad for PIN entry (design D10): digits and backspace as [Sizes.minTouchTarget]
 * touch targets, masked entry dots above, a full-width filled submit button below. Purely
 * presentational; the caller owns the entered digits and every callback.
 */
@Composable
fun PinKeypad(
    enteredLength: Int,
    maxLength: Int,
    enabled: Boolean,
    submitEnabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        PinDots(enteredLength = enteredLength, maxLength = maxLength)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            for (row in listOf("123", "456", "789")) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    for (digit in row) {
                        DigitKey(digit = digit, enabled = enabled, onClick = { onDigit(digit) })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Box(Modifier.size(Sizes.minTouchTarget))
                DigitKey(digit = '0', enabled = enabled, onClick = { onDigit('0') })
                FilledTonalButton(
                    onClick = onBackspace,
                    enabled = enabled,
                    modifier = Modifier.size(Sizes.minTouchTarget),
                    contentPadding = PaddingValues(Spacing.none),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_backspace),
                        contentDescription = stringResource(R.string.applock_keypad_backspace),
                    )
                }
            }
        }

        Button(
            onClick = onSubmit,
            enabled = submitEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.primaryActionHeight),
        ) {
            Text(stringResource(R.string.applock_keypad_submit))
        }
    }
}

@Composable
private fun DigitKey(digit: Char, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(Sizes.minTouchTarget),
        contentPadding = PaddingValues(Spacing.none),
    ) {
        Text(
            text = digit.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TabularNumbers),
        )
    }
}

@Composable
private fun PinDots(enteredLength: Int, maxLength: Int) {
    val description = stringResource(R.string.applock_pin_dots_description, enteredLength, maxLength)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        repeat(maxLength) { index ->
            val filled = index < enteredLength
            Box(
                modifier = Modifier
                    .size(Sizes.pinDot)
                    .then(
                        if (filled) {
                            Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                        } else {
                            Modifier.border(Sizes.pinDotStroke, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        },
                    ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PinKeypadPreview() {
    PillsnerTheme {
        Surface {
            PinKeypad(
                enteredLength = 2,
                maxLength = 6,
                enabled = true,
                submitEnabled = false,
                onDigit = {},
                onBackspace = {},
                onSubmit = {},
            )
        }
    }
}

@Preview(fontScale = 2f)
@Composable
private fun PinKeypadLargeFontPreview() {
    PillsnerTheme {
        Surface {
            PinKeypad(
                enteredLength = 4,
                maxLength = 6,
                enabled = true,
                submitEnabled = true,
                onDigit = {},
                onBackspace = {},
                onSubmit = {},
            )
        }
    }
}
