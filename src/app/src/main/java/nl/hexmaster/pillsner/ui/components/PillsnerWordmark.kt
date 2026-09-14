package nl.hexmaster.pillsner.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme

/**
 * The fragment the name borrows from "Partner". Everything before it is the "Pills" half.
 */
private const val ACCENT_FRAGMENT = "ner"

/**
 * The app name split into the two words it blends, "Pills" and Part"ner".
 *
 * The leading fragment is drawn in `secondary` and the trailing fragment in `primary`, so the blend
 * the name is built from is visible. The colouring is decoration only: it carries no meaning, the
 * two fragments sit flush against each other with no gap, and they share one type role and one
 * weight, so the wordmark reads as a single word. It is one [Text] over one annotated string, which
 * keeps it a single node for TalkBack, for text selection and for test matchers.
 *
 * @param style the type role to draw it at. Display roles use Raleway at weight 200, which the
 *   design system (section 3.3) forbids below 24 sp, so keep this at a display or a large headline
 *   role and never step it down to a title or body role.
 */
@Composable
fun PillsnerWordmark(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayLarge,
) {
    val title = stringResource(R.string.app_title)
    val (lead, accent) = splitWordmark(title)
    val leadColor = MaterialTheme.colorScheme.secondary
    val accentColor = MaterialTheme.colorScheme.primary

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = leadColor)) { append(lead) }
            if (accent.isNotEmpty()) {
                withStyle(SpanStyle(color = accentColor)) { append(accent) }
            }
        },
        style = style,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

/**
 * Splits [title] into the leading fragment and the accent fragment at the last occurrence of
 * [ACCENT_FRAGMENT].
 *
 * The name comes from a string resource, so this stays tolerant: when the title does not contain
 * the fragment, or contains nothing but it, the whole title is returned as the lead and the accent
 * is empty. A renamed app then draws as a plain one-colour title instead of rendering half a word.
 */
internal fun splitWordmark(title: String): Pair<String, String> {
    val index = title.lastIndexOf(ACCENT_FRAGMENT)
    if (index <= 0) return title to ""
    return title.substring(0, index) to title.substring(index)
}

@PreviewLightDark
@Composable
private fun PillsnerWordmarkPreview() {
    PillsnerTheme {
        Surface { PillsnerWordmark() }
    }
}
