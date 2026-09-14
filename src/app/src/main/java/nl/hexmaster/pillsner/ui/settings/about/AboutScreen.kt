package nl.hexmaster.pillsner.ui.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.AppInfo
import nl.hexmaster.pillsner.ui.components.PillsnerWordmark
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Stable tags for the About screen's content, for semantics tests. */
object AboutScreenTestTags {
    const val TITLE = "about_title"
    const val BACK = "about_back"
    const val NAME_ORIGIN = "about_name_origin"
    const val VERSION = "about_version"
    const val APPLICATION_ID = "about_application_id"
    const val AUTHOR = "about_author"
}

/**
 * The About screen (app-about-screen design D3, D5): what this app is, which build is installed,
 * and who wrote it.
 *
 * No view model, deliberately. Everything here is constant for the lifetime of the process, so a
 * view model would hold a value it never changes and expose a flow that never emits; [appInfo]
 * comes down from the app shell instead. The first genuinely stateful thing added to this screen
 * reverses that.
 *
 * Nothing on the screen navigates anywhere: the fact rows are plain, unclickable list items, and
 * the back affordance is the only control.
 *
 * @param appInfo what the build says about itself.
 * @param onBack leaves the screen; both this and the system back return to Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    appInfo: AppInfo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.about_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag(AboutScreenTestTags.TITLE),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(AboutScreenTestTags.BACK)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { contentPadding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val isWide = maxWidth >= Spacing.contentMaxWidth
            val sidePadding = if (isWide) Spacing.screenEdgeWide else Spacing.screenEdge

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = Spacing.contentMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = sidePadding)
                    .padding(top = Spacing.xl, bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                IdentityBlock(name = appInfo.name)

                Text(
                    text = stringResource(R.string.about_name_origin),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag(AboutScreenTestTags.NAME_ORIGIN),
                )

                Column {
                    FactRow(
                        label = stringResource(R.string.about_version_label),
                        value = stringResource(
                            R.string.about_version_value,
                            appInfo.versionName,
                            appInfo.versionCode,
                        ),
                        testTag = AboutScreenTestTags.VERSION,
                    )
                    FactRow(
                        label = stringResource(R.string.about_application_id_label),
                        value = appInfo.applicationId,
                        testTag = AboutScreenTestTags.APPLICATION_ID,
                    )
                    FactRow(
                        label = stringResource(R.string.about_author_label),
                        value = stringResource(R.string.about_author),
                        testTag = AboutScreenTestTags.AUTHOR,
                    )
                }
            }
        }
    }
}

/**
 * The product mark with the app's name beneath it. The mark carries the name as its content
 * description because the mark is what identifies the block; the wordmark below it is a separate
 * node, so a screen reader announces the name twice, which an identity block can afford.
 */
@Composable
private fun IdentityBlock(name: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_pillsner_logo),
            contentDescription = name,
            modifier = Modifier.size(Sizes.logoHeader),
        )
        // The one place the name is drawn is the shared wordmark (design system 8.13); no screen
        // re-implements the Pills/ner split.
        PillsnerWordmark(style = MaterialTheme.typography.headlineMedium)
    }
}

/**
 * One labelled fact. A `ListItem` with a headline and supporting content, which a screen reader
 * already reads as label then value, and which wraps rather than truncates at any font scale
 * because neither text sets `maxLines`.
 */
@Composable
private fun FactRow(label: String, value: String, testTag: String, modifier: Modifier = Modifier) {
    ListItem(
        headlineContent = {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
        },
        supportingContent = {
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.testTag(testTag),
    )
}

@PreviewLightDark
@Composable
private fun AboutScreenPreview() {
    PillsnerTheme {
        AboutScreen(appInfo = PreviewAppInfo, onBack = {})
    }
}

@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun AboutScreenLargeFontPreview() {
    PillsnerTheme {
        AboutScreen(appInfo = PreviewAppInfo, onBack = {})
    }
}

internal val PreviewAppInfo = AppInfo(
    name = "Pillsner",
    applicationId = "nl.hexmaster.pillsner",
    versionName = "0.1.0",
    versionCode = 1,
)
