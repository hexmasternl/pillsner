package nl.hexmaster.pillsner.ui.medicines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.ui.home.EmptyState
import nl.hexmaster.pillsner.ui.navigation.NavigationTestTags
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Test tags for semantics tests of the Medicines screen. */
object MedicinesScreenTestTags {
    const val LIST = "medicines_list"
    const val ADD_FAB = "medicines_add_fab"
    const val ACTIVE_HEADER = "medicines_active_header"
    const val INACTIVE_HEADER = "medicines_inactive_header"
    const val TILE = "medicines_tile"
    const val INACTIVE_CHIP = "medicines_inactive_chip"
    const val ACTIVE_EMPTY_STATE = "medicines_active_empty_state"
    const val EMPTY_STATE = "medicines_empty_state"
    const val SNACKBAR = "medicines_snackbar"
}

/**
 * The Medicines destination (design D5): one scrolling list holding the screen title, the Active
 * section, then the Inactive section, with a large floating action button that starts adding a
 * medicine.
 *
 * One list rather than two is deliberate: at the largest font scale everything scrolls together
 * and a screen reader gets a single natural reading order.
 */
@Composable
fun MedicinesScreen(
    uiState: MedicinesUiState,
    onAddMedicine: () -> Unit,
    modifier: Modifier = Modifier,
    onSetActive: (MedicationId, Boolean) -> Unit = { _, _ -> },
    onOpenMedication: (MedicationId) -> Unit = {},
    effects: Flow<MedicinesEffect> = emptyFlow(),
) {
    val formatter = rememberScheduleDescriptionFormatter()
    val snackbarHostState = remember { SnackbarHostState() }
    val updateFailed = stringResource(R.string.medicines_update_failed)
    val openFailed = stringResource(R.string.medication_form_open_failed)

    // Gesture state, not domain state: which tile is currently pulled aside. Only one at a time,
    // so revealing another closes the previous one.
    var revealedId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                MedicinesEffect.UpdateFailed -> snackbarHostState.showSnackbar(updateFailed)
                MedicinesEffect.OpenFailed -> snackbarHostState.showSnackbar(openFailed)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = {
            SnackbarHost(snackbarHostState, Modifier.testTag(MedicinesScreenTestTags.SNACKBAR))
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMedicine,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag(MedicinesScreenTestTags.ADD_FAB),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.medicines_add_content_description),
                    modifier = Modifier.size(Sizes.iconDefault),
                )
            }
        },
    ) { innerPadding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val isWide = maxWidth >= Spacing.contentMaxWidth
            val sidePadding = if (isWide) Spacing.screenEdgeWide else Spacing.screenEdge
            val hasAnyMedicine = uiState.active.isNotEmpty() || uiState.inactive.isNotEmpty()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = Spacing.contentMaxWidth)
                    .testTag(MedicinesScreenTestTags.LIST),
                contentPadding = PaddingValues(
                    start = sidePadding,
                    end = sidePadding,
                    // The last tile must clear the floating action button (section 5).
                    bottom = Sizes.fab + Spacing.lg,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                item(key = "title") {
                    Text(
                        text = stringResource(R.string.medicines_title),
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier
                            .semantics { heading() }
                            .testTag(NavigationTestTags.MEDICINES_TITLE),
                    )
                }

                when {
                    uiState.isLoading -> Unit

                    !hasAnyMedicine -> item(key = "empty") {
                        EmptyState(
                            icon = R.drawable.ic_medication,
                            title = stringResource(R.string.medicines_empty_title),
                            hint = stringResource(R.string.medicines_empty_hint),
                            modifier = Modifier.testTag(MedicinesScreenTestTags.EMPTY_STATE),
                        )
                    }

                    else -> {
                        stickyHeader(key = "active_header") {
                            MedicinesSectionHeader(
                                title = stringResource(R.string.medicines_section_active),
                                modifier = Modifier.testTag(MedicinesScreenTestTags.ACTIVE_HEADER),
                            )
                        }
                        if (uiState.active.isEmpty()) {
                            item(key = "active_empty") {
                                EmptyState(
                                    icon = R.drawable.ic_medication,
                                    title = stringResource(R.string.medicines_active_empty_title),
                                    hint = stringResource(R.string.medicines_active_empty_hint),
                                    modifier = Modifier.testTag(MedicinesScreenTestTags.ACTIVE_EMPTY_STATE),
                                )
                            }
                        } else {
                            items(uiState.active, key = { it.id.value }) { tile ->
                                val descriptions = remember(tile.schedules, formatter) {
                                    tile.schedules.map { formatter.describe(it.summary, it.amount) }
                                }
                                SwipeableMedicineTile(
                                    tile = tile,
                                    descriptions = descriptions,
                                    isRevealed = revealedId == tile.id.value,
                                    onRevealChange = { revealed ->
                                        revealedId = when {
                                            revealed -> tile.id.value
                                            // Another tile may already have taken over by the time
                                            // this one finishes closing; do not undo that.
                                            revealedId == tile.id.value -> null
                                            else -> revealedId
                                        }
                                    },
                                    onSetActive = { active ->
                                        revealedId = null
                                        onSetActive(tile.id, active)
                                    },
                                    onOpen = {
                                        revealedId = null
                                        onOpenMedication(tile.id)
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }

                        if (uiState.inactive.isNotEmpty()) {
                            stickyHeader(key = "inactive_header") {
                                MedicinesSectionHeader(
                                    title = stringResource(R.string.medicines_section_inactive),
                                    modifier = Modifier.testTag(MedicinesScreenTestTags.INACTIVE_HEADER),
                                )
                            }
                            items(uiState.inactive, key = { it.id.value }) { tile ->
                                val descriptions = remember(tile.schedules) {
                                    tile.schedules.map { formatter.describe(it.summary, it.amount) }
                                }
                                SwipeableMedicineTile(
                                    tile = tile,
                                    descriptions = descriptions,
                                    isRevealed = revealedId == tile.id.value,
                                    onRevealChange = { revealed ->
                                        revealedId = when {
                                            revealed -> tile.id.value
                                            // Another tile may already have taken over by the time
                                            // this one finishes closing; do not undo that.
                                            revealedId == tile.id.value -> null
                                            else -> revealedId
                                        }
                                    },
                                    onSetActive = { active ->
                                        revealedId = null
                                        onSetActive(tile.id, active)
                                    },
                                    onOpen = {
                                        revealedId = null
                                        onOpenMedication(tile.id)
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One tile with its swipe-to-reveal action, used by both sections so the gesture behaves the same
 * wherever the medicine happens to sit.
 */
@Composable
private fun SwipeableMedicineTile(
    tile: MedicineTileState,
    descriptions: List<String>,
    isRevealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    onSetActive: (Boolean) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SwipeRevealTile(
        isRevealed = isRevealed,
        onRevealChange = onRevealChange,
        modifier = modifier,
        onClick = onOpen,
        action = {
            MedicineTileAction(isActive = tile.isActive, onClick = { onSetActive(!tile.isActive) })
        },
    ) {
        MedicineTile(
            state = tile,
            descriptions = descriptions,
            onSetActive = onSetActive,
            onClick = onOpen,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MedicinesScreenTestTags.TILE),
        )
    }
}

// Preview data: invented names covering every schedule shape.
private fun previewLine(summary: ScheduleSummary, amount: String, unit: DoseUnit) =
    ScheduleLine(summary, Quantity.of(amount, unit))

private val previewActive = listOf(
    MedicineTileState(
        MedicationId(1),
        "Amoxicillin",
        listOf(previewLine(ScheduleSummary.EveryNHours(8), "500", DoseUnit.MILLIGRAM)),
        isActive = true,
    ),
    MedicineTileState(
        MedicationId(2),
        "Metoprolol",
        listOf(
            previewLine(ScheduleSummary.EveryNHours(12), "40", DoseUnit.MILLIGRAM),
            previewLine(
                ScheduleSummary.TimesPerDayOnDays(1, setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
                "20",
                DoseUnit.MILLIGRAM,
            ),
        ),
        isActive = true,
    ),
    MedicineTileState(
        MedicationId(3),
        "paracetamol",
        listOf(previewLine(ScheduleSummary.AsNeeded, "500", DoseUnit.MILLIGRAM)),
        isActive = true,
    ),
)
private val previewInactive = listOf(
    MedicineTileState(
        MedicationId(4),
        "Methotrexate",
        listOf(
            previewLine(
                ScheduleSummary.TimesPerDayOnDays(
                    1,
                    setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                ),
                "1",
                DoseUnit.TABLET,
            ),
        ),
        isActive = false,
    ),
    MedicineTileState(
        MedicationId(5),
        "Vitamin D",
        listOf(previewLine(ScheduleSummary.TimesEveryOtherDay(1), "2.5", DoseUnit.MILLILITRE)),
        isActive = false,
    ),
)

@PreviewLightDark
@Composable
private fun MedicinesScreenEmptyPreview() {
    PillsnerTheme {
        Surface { MedicinesScreen(MedicinesUiState(isLoading = false), onAddMedicine = {}) }
    }
}

@PreviewLightDark
@Composable
private fun MedicinesScreenActiveOnlyPreview() {
    PillsnerTheme {
        Surface {
            MedicinesScreen(MedicinesUiState(active = previewActive, isLoading = false), onAddMedicine = {})
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun MedicinesScreenMixedPreview() {
    PillsnerTheme {
        Surface {
            MedicinesScreen(
                MedicinesUiState(active = previewActive, inactive = previewInactive, isLoading = false),
                onAddMedicine = {},
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun MedicineTileRevealedPreview() {
    PillsnerTheme {
        Surface {
            Column(
                Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                RevealedTilePreview(previewActive.first())
                RevealedTilePreview(previewInactive.first())
            }
        }
    }
}

@Preview(name = "Right to left", locale = "ar")
@Composable
private fun MedicineTileRevealedRtlPreview() {
    PillsnerTheme {
        Surface {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.padding(Spacing.lg)) {
                    RevealedTilePreview(previewActive.first())
                }
            }
        }
    }
}

/** A tile already pulled aside, so the action behind it is what the preview shows. */
@Composable
private fun RevealedTilePreview(tile: MedicineTileState) {
    Box {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            MedicineTileAction(isActive = tile.isActive, onClick = {})
        }
        MedicineTile(
            state = tile,
            descriptions = listOf("40 mg every 12 hours"),
            modifier = Modifier.fillMaxWidth(MedicineTilePreviewWidthFraction),
        )
    }
}

/** Enough of the tile pushed aside for the action to show in a static preview. */
private const val MedicineTilePreviewWidthFraction = 0.7f
