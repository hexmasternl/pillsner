## Context

Settings today has two sections, Language and Security, each a self-contained composable in a `LazyColumn` inside `SettingsScreen`. Secondary screens (PIN setup, the medicine form) are non-top-level destinations in the one `NavHost` in `PillsnerApp.kt`; the navigation suite hides itself on any route that is not one of the three top-level destinations, so a new secondary destination costs nothing extra.

The facts the About screen shows — version name, version code, application id — live in `src/app/build.gradle.kts` (`versionName = "0.1.0"`, `versionCode = 1`, `applicationId = "nl.hexmaster.pillsner"`). The app does not currently generate a `BuildConfig` class: `buildFeatures` enables only `compose`. The app name and the author are fixed text.

Constraints from the repository: the domain layer stays free of Android dependencies; user-facing text lives in string resources with a complete Dutch translation or lint fails; every visual decision comes from a theme token in `docs/design-system.md`; nothing leaves the device.

## Goals / Non-Goals

**Goals:**

- A discoverable About entry on Settings that also shows the installed version without opening anything.
- An About screen that answers "what is this, which version, who wrote it, why that name" in one glance, readable at 200 % font scale and with a screen reader.
- Version and identity read from the build in one place, exposed as a plain domain value, so no composable ever touches a generated constant.
- Nothing new that the privacy promise has to account for: no permission, no network, no link out, no dependency.

**Non-Goals:**

- Licence or open-source attribution lists, changelogs, "what's new", a link to a repository or a website, a way to contact the author, a debug or diagnostics panel, or a build timestamp. Each is a separate proposal if it is ever wanted.
- Showing the git commit or build type. The version name and code are the contract with the user; anything finer is developer detail.
- Changing how versions are numbered, or automating `versionName`.

## Decisions

### D1. `AppInfo`: a domain value, filled once in the data layer

A data class in `domain/model/AppInfo.kt` with four fields: `name`, `applicationId`, `versionName`, `versionCode`. No Android imports, so anything that renders it is unit-testable.

`data/appinfo/BuildConfigAppInfoProvider.kt` builds the single instance from the generated constants and the app-name resource. `AppContainer` exposes it as `val appInfo: AppInfo`, created eagerly — it is four values and cannot fail.

Alternative considered: reading `PackageManager.getPackageInfo` at runtime. Rejected: it needs a `Context`, a nullable `versionName`, an exception path for a package that by definition exists, and an API-level split for `longVersionCode` — all to obtain the same four values the build already knows.

### D2. Enable the `buildConfig` build feature

`android.buildFeatures { buildConfig = true }` in `src/app/build.gradle.kts`, so `BuildConfig.APPLICATION_ID`, `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE` are generated. This is a build flag, not a dependency; it adds one generated class and no runtime cost. The app name stays `R.string.app_name`, which already exists.

Alternative considered: hard-coding the four values in Kotlin. Rejected: they would drift from the build on the first version bump, which is exactly the failure the screen exists to prevent.

### D3. No view model for About

The About screen's content is constant for the lifetime of the process. There is no state to hold, no event to handle, nothing to survive a configuration change beyond what the route already carries. `PillsnerAppContent` gains an `appInfo: AppInfo` parameter and passes it to the screen and to the Settings section; the screen is a pure function of its input.

This is a deliberate, narrow departure from the MVVM convention in `CLAUDE.md`: a view model here would hold a value it never changes and expose a flow that never emits. Data still flows one way, down. Any future About content that is genuinely stateful (a licence list loaded from assets, say) introduces a view model then.

### D4. Settings gains an About section, last

`ui/settings/about/AboutSection.kt`: a `headlineSmall` header reading "About", then one `ListItem` whose headline is "About Pillsner" (`titleSmall`) and whose supporting text is the version summary, with an `ic_chevron_right` trailing icon — the same row shape the Security section's "Change PIN" already uses, so Settings stays visually one list. The row is `clickable(role = Role.Button)` with `heightIn(min = Sizes.minTouchTarget)`.

It is added as the last `item` in the `SettingsScreen` `LazyColumn`, after Security. `SettingsScreen` gains an `appInfo` parameter and an `onAboutTapped` lambda.

Version summary format: "Version 0.1.0 (1)" — name first, code in parentheses. One string resource with two placeholders, so Dutch can phrase and order it independently.

### D5. The About screen

A `Scaffold` with a `TopAppBar` (title "About" in `titleLarge`, `ic_arrow_back` navigation icon), matching `PinSetupScreen`. Design system 8.7: secondary screens use a top app bar, never a display title.

Content is a single scrolling `Column` (`verticalScroll`), constrained to `Spacing.contentMaxWidth`, with `Spacing.screenEdge` / `screenEdgeWide` side padding as the other screens do:

1. **Identity block**, centered: `ic_pillsner_logo` at 96 dp with the app name as its `contentDescription`, then the name "Pillsner" in `headlineMedium` a `Spacing.lg` below it.
2. **Name origin**, one `bodyLarge` paragraph explaining that Pillsner blends *Pills* and *Partner* — a partner that remembers the doses so the user does not have to. Left-aligned running text that wraps.
3. **Facts list**: three label and value rows — Version, Application id, Author — each a `ListItem` with the label as a `titleSmall` headline and the value as `bodyMedium` supporting content. Not clickable, no chevron; nothing on this screen navigates anywhere.

Everything wraps; nothing sets `maxLines`. At 200 % font scale the column scrolls and values wrap under their labels rather than truncating.

Alternative considered: a centered card holding all of it. Rejected: a list matches the screen the user just came from and survives large fonts more predictably.

### D6. Route and navigation

`@Serializable data object About` in `ui/navigation/Routes.kt`, registered as `composable<About>` in the `NavHost` in `PillsnerApp.kt`. It is not a top-level destination, so the existing `onTopLevelDestination` check hides the navigation bar or rail for free. Settings navigates with `navController.navigate(About)`; the back arrow and the system back both pop back to Settings with its navigation item still selected.

The route carries no arguments — the content comes from `appInfo`, which is identical for every process — so process death restores it with nothing to rebuild.

### D7. Accessibility and semantics

- The "About" section header on Settings is a heading (`semantics { heading() }`), as the Language and Security headers are.
- The Settings row announces its label and that it is a button, and opens the screen with a single activation.
- Each fact row announces as "label, value"; a `ListItem` with headline and supporting content already reads that way.
- The logo carries the app name as its `contentDescription` because it is what identifies the block; the "Pillsner" text beneath it is a separate node, so the name is announced at most twice, which is acceptable for an identity block.

Test tags: `AboutSectionTestTags.ABOUT_ROW`, and `AboutScreenTestTags` for the title, the version value, the application-id value and the author value.

### D8. Strings

New keys in `values/strings.xml` and `values-nl/strings.xml`: the section header, the row title, the version summary (two placeholders), the screen title, the name-origin paragraph, and the three fact labels. The values themselves — "Pillsner", "nl.hexmaster.pillsner", "Eduard Keilholz" — are not translated: the first comes from `app_name`, the other two from `AppInfo`. The name-origin paragraph is translated because it is running prose, but the words *Pills* and *Partner* stay English in both languages, because the pun is on the English words.

### D9. Package layout

```
domain/model/AppInfo.kt
data/appinfo/BuildConfigAppInfoProvider.kt
ui/settings/about/AboutSection.kt
ui/settings/about/AboutScreen.kt
```

About sits under `ui/settings/` because Settings is the only way in, mirroring `ui/settings/language/`.

## Risks / Trade-offs

- **The version shown is whatever `build.gradle.kts` says, and nothing enforces a bump** → Accepted. It is strictly better than no version at all, and the release checklist owns the bump. The screen reports the build faithfully, which is all it can promise.
- **`buildConfig = true` generates a class that release minification must keep** → `BuildConfig` constants are inlined at the use site, so R8 has nothing to strip that matters. No ProGuard rule is needed; the release build is verified as part of this change.
- **No view model on About departs from the MVVM convention** → Stated in D3 and scoped to this screen, so a reviewer finds the reason in one place, and the first stateful addition reverses it.
- **The name-origin copy is product voice, not a fact carried by an existing spec** → This change's spec fixes the copy, so it is reviewed like any other behaviour rather than invented during implementation.

## Migration Plan

No data, no schema, no persisted setting. The change is additive: one build flag, one route, one section, one screen. Rolling it back is deleting the new files and reverting four call sites.

## Open Questions

None.
