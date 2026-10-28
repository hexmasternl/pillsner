package nl.hexmaster.pillsner.wear.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction

/**
 * How many items a watch column holds, composed or not, read from its `CollectionInfo`.
 *
 * On a small round watch a lazy column composes only what is near the screen, so counting tagged
 * nodes counts what happens to be composed, not what the screen contains (#104).
 */
internal fun SemanticsNodeInteraction.itemCount(): Int =
    fetchSemanticsNode().config[SemanticsProperties.CollectionInfo].rowCount
