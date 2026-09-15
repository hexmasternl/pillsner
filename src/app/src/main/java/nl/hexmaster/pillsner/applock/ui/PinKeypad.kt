package nl.hexmaster.pillsner.applock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import nl.hexmaster.pillsner.applock.domain.Pin
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing
import nl.hexmaster.pillsner.ui.theme.TabularNumbers

/** The three rows above the zero row. Declared once so the layout below stays readable. */
private val DIGIT_ROWS = listOf("123", "456", "789")

/**
 * A numeric keypad for PIN entry (design D10): [Sizes.pinKey] round keys laid out three to a row
 * like the device's own lock screen, masked entry dots above, a full-width filled submit button
 * below. The keys divide the available width, so the same keypad fits a full screen and the
 * identity-check dialog.
 *
 * The dots show [minLength] slots until more digits than that have been entered, and then one slot
 * per entered digit up to [maxLength]. That is deliberate: a fixed row of [maxLength] slots reads
 * as a demand for exactly that many digits, which is not the rule (see [Pin]). Purely
 * presentational; the caller owns the entered digits and every callback.
 */
@Composable
fun PinKeypad(
    enteredLength: Int,
    enabled: Boolean,
    submitEnabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    minLength: Int = Pin.MIN_LENGTH,
    maxLength: Int = Pin.MAX_LENGTH,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        PinDots(enteredLength = enteredLength, minLength = minLength, maxLength = maxLength)

        Column(
            modifier = Modifier
                .widthIn(max = Sizes.pinKeypadMaxWidth)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            for (row in DIGIT_ROWS) {
                KeyRow {
                    for (digit in row) {
                        DigitKey(
                            digit = digit,
                            enabled = enabled,
                            onClick = { onDigit(digit) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            KeyRow {
                Spacer(Modifier.weight(1f))
                DigitKey(
                    digit = '0',
                    enabled = enabled,
                    onClick = { onDigit('0') },
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = onBackspace,
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = Sizes.minTouchTarget)
                        .heightIn(min = Sizes.pinKey),
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
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun DigitKey(
    digit: Char,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        // The width comes from the row's weight; the floor keeps a key a legal touch target even
        // on a container narrower than the keypad was designed for (design system section 10).
        modifier = modifier
            .widthIn(min = Sizes.minTouchTarget)
            .heightIn(min = Sizes.pinKey),
        contentPadding = PaddingValues(Spacing.none),
    ) {
        Text(
            text = digit.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TabularNumbers),
        )
    }
}

@Composable
private fun PinDots(enteredLength: Int, minLength: Int, maxLength: Int) {
    val description = stringResource(
        R.string.applock_pin_dots_description,
        enteredLength,
        minLength,
        maxLength,
    )
    // Never fewer slots than the minimum, so the row says "at least this many"; one more slot per
    // digit beyond it, so the row never demands digits the user does not have to give.
    val slots = enteredLength.coerceIn(minLength, maxLength)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        repeat(slots) { index ->
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
                enabled = true,
                submitEnabled = false,
                onDigit = {},
                onBackspace = {},
                onSubmit = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PinKeypadFullPreview() {
    PillsnerTheme {
        Surface {
            PinKeypad(
                enteredLength = 6,
                enabled = true,
                submitEnabled = true,
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
                enabled = true,
                submitEnabled = true,
                onDigit = {},
                onBackspace = {},
                onSubmit = {},
            )
        }
    }
}
