package nl.hexmaster.pillsner.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Test tags for the reminder readiness banner. */
object ReminderBannerTestTags {
    const val BANNER = "reminder_banner"
    const val ACTION = "reminder_banner_action"
}

/**
 * Says so when reminders cannot be delivered as promised (docs/design-system.md section 8.9).
 *
 * This is the one red surface the user does not ask for, and deliberately so: an app that quietly
 * fails to remind someone of their medicine is worse than one that admits it. It appears only when
 * notifications are off or exact alarms are unavailable.
 */
@Composable
fun ReminderBanner(
    message: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ReminderBannerTestTags.BANNER),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            Modifier.padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_notifications_off),
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconDefault),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(message, style = MaterialTheme.typography.bodyLarge)
                TextButton(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
                    modifier = Modifier
                        .heightIn(min = Sizes.minTouchTarget)
                        .testTag(ReminderBannerTestTags.ACTION),
                ) {
                    Text(stringResource(R.string.reminder_banner_open_settings))
                }
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun ReminderBannerPreview() {
    PillsnerTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                ReminderBanner(
                    message = "Pillsner cannot show reminders because notifications are turned off.",
                    onOpenSettings = {},
                )
                ReminderBanner(
                    message = "Reminders may arrive up to ten minutes late because exact alarms are turned off.",
                    onOpenSettings = {},
                )
            }
        }
    }
}
