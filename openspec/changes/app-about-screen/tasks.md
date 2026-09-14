## 1. Verify the ground

- [x] 1.1 Confirm the scaffold exists: `src/settings.gradle.kts`, the `app` module, `ui/PillsnerApp.kt`, `ui/settings/SettingsScreen.kt`, `ui/navigation/Routes.kt` and `di/AppContainer.kt`. Stop and report if any is missing — this change creates none of them.
- [x] 1.2 Confirm `src/app/src/main/res/drawable/ic_pillsner_logo.xml`, `ic_chevron_right.xml` and `ic_arrow_back.xml` are present, and that `R.string.app_name` reads "Pillsner".

## 2. App identity

- [x] 2.1 Enable `buildConfig = true` in the `buildFeatures` block of `src/app/build.gradle.kts`, alongside `compose`.
- [x] 2.2 Add `domain/model/AppInfo.kt`: a data class with `name`, `applicationId`, `versionName` and `versionCode`, KDoc'd, with no Android imports.
- [x] 2.3 Add `data/appinfo/BuildConfigAppInfoProvider.kt` that builds one `AppInfo` from `BuildConfig.APPLICATION_ID`, `BuildConfig.VERSION_NAME`, `BuildConfig.VERSION_CODE` and the `app_name` resource (design D1, D2).
- [x] 2.4 Expose `val appInfo: AppInfo` from `di/AppContainer.kt`.
- [x] 2.5 Unit-test the provider: the reported values equal the build constants and the app-name resource.

## 3. Strings

- [x] 3.1 Add the English strings to `values/strings.xml`: About section header, "About Pillsner" row title, version summary with two placeholders ("Version %1$s (%2$d)"), About screen title, the name-origin paragraph, and the labels Version, Application id and Author.
- [x] 3.2 Add the Dutch counterparts to `values-nl/strings.xml`, keeping *Pills* and *Partner* in English inside the paragraph (design D8).

## 4. About screen

- [x] 4.1 Add `ui/settings/about/AboutScreen.kt` with a `TopAppBar` (title in `titleLarge`, `ic_arrow_back` back affordance) and a scrolling `Column` constrained to `Spacing.contentMaxWidth` with `Spacing.screenEdge` / `screenEdgeWide` side padding (design D5).
- [x] 4.2 Build the identity block: `ic_pillsner_logo` at 96 dp with the app name as its `contentDescription`, the name in `headlineMedium` below it, both centered.
- [x] 4.3 Add the name-origin paragraph in `bodyLarge`, left-aligned, no `maxLines`.
- [x] 4.4 Add the three fact rows (Version as "name (code)", Application id, Author) as non-clickable `ListItem`s with `titleSmall` labels and `bodyMedium` values, each with a test tag from `AboutScreenTestTags`.
- [x] 4.5 Ship `@PreviewLightDark` and a `fontScale = 2f` preview for the screen.

## 5. Settings section

- [x] 5.1 Add `ui/settings/about/AboutSection.kt`: `headlineSmall` header marked as a heading, then one clickable `ListItem` ("About Pillsner", version summary, `ic_chevron_right`) with `Role.Button`, `heightIn(min = Sizes.minTouchTarget)` and `AboutSectionTestTags.ABOUT_ROW` (design D4, D7).
- [x] 5.2 Add `appInfo` and `onAboutTapped` parameters to `SettingsScreen` and render the section as the last `item` of its `LazyColumn`, after Security.
- [x] 5.3 Ship `@PreviewLightDark` and a large-font preview for the section, and update the existing `SettingsScreen` preview with the new parameters.

## 6. Navigation

- [x] 6.1 Add `@Serializable data object About` to `ui/navigation/Routes.kt` with KDoc saying it is a secondary destination reached only from Settings.
- [x] 6.2 Pass `appInfo` into `PillsnerApp` and `PillsnerAppContent`, register `composable<About>` in the `NavHost`, and wire the Settings row to `navController.navigate(About)` and the screen's back affordance to `popBackStack()` (design D6).
- [x] 6.3 Update `MainActivity` (or whatever composes `PillsnerApp`) to pass `AppContainer.appInfo`.

## 7. Tests

- [x] 7.1 Compose test: the About row appears on Settings below Security, shows the version summary, and announces as a button.
- [x] 7.2 Compose test: the About screen shows the name, the origin paragraph, the version, the application id and "Eduard Keilholz", and each fact row announces as label then value.
- [x] 7.3 Compose test at `fontScale = 2f`: every element on the About screen is reachable by scrolling and no text is truncated.
- [x] 7.4 Navigation test: tapping the row opens About with the navigation bar hidden; back returns to Settings with the Settings item selected; a second back reaches Home.

## 8. Design and verification

- [x] 8.1 Run the `pillsner-ui-review` skill over `ui/settings/about/` and `ui/settings/SettingsScreen.kt`, and fix every violation it reports.
- [ ] 8.2 Run the unit test task and the lint task from `src/`; confirm no missing-translation error and report any failure verbatim.
- [ ] 8.3 Assemble the release build to confirm `buildConfig = true` needs no ProGuard rule (design risk 2).
- [x] 8.4 Check the manifest diff: no permission added, no network, no new dependency in the version catalog.
