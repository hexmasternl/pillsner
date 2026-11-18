# Design

## Context

The watch theme (`ui/theme/WearTheme.kt`) copies the dark column of `docs/design-system.md` §2.2. It maps the design system's dark `background` and `surfaceContainerLow` to Deep Moss `#101413`. Both screens wrap their content in Wear Compose Material 3's `AppScaffold` and `ScreenScaffold` (`wearCompose` 1.6.2), which fill with `MaterialTheme.colorScheme.background` and set no colour of their own. The window theme `Theme.PillsnerWear` paints `@color/deep_moss` as `android:windowBackground`. The app has no splash library, no tiles and no complications. The watch's minSdk is 26 (`src/gradle/libs.versions.toml`), so it supports Wear OS versions on both sides of API 31, where the platform splash screen was introduced. Dose cards fill with `secondaryContainer` (`#1D4E7F`) or `errorContainer` (`#93000A`). Secondary text uses `onSurfaceVariant` (`#BFC9C4`).

## Goals / Non-Goals

**Goals:**
- Make black the only background colour the watch can show behind its content, enforced by tests.
- Write the watch's colour rules into the design system.

**Non-Goals:**
- Changing the phone's dark theme. The phone keeps Deep Moss, because the tinted background is right for phones.
- Retuning the watch's accent or container colours. They already meet contrast on black (D3).
- Adding a tile.
- Changing the launcher icon. Its green background is the icon's own layer, not the screen background.

## Decisions

**D1. Set `background` and `surfaceContainerLow` to `Color.Black` in the watch palette.** Wear Material 3's baseline dark scheme builds from black: its `background` is black, and so are the lowest surface roles. `surfaceContainerLow` is set to black too, because any component that defaults to it would otherwise paint a tinted panel that looks like background. `surfaceContainer` and `surfaceContainerHigh` keep their tones so that raised elements still separate from the canvas.
*Alternative considered:* override `containerColor` on each `AppScaffold` and `ScreenScaffold`. Rejected because it fixes two call sites but leaves the next screen, dialog or confirmation on the tinted colour. Fixing it in the theme covers them all.

**D2. Make the window background black, and show the startup icon with `androidx.core:core-splashscreen`.** Set `android:windowBackground` to `@android:color/black` and remove the `deep_moss` entry once nothing references it. That alone doesn't satisfy WO-V15. The watch's minSdk is 26, and before API 31 the platform's starting window draws only the window background, with no icon. From API 31 the platform draws an icon, but at a size it picks itself.

**Reason for the dependency, as CLAUDE.md requires:** Google's [Wear OS splash screen guide](https://developer.android.com/training/wearables/apps/splash-screen) prescribes the Jetpack SplashScreen library "to ensure it displays correctly on all Wear OS versions". It is first-party AndroidX, back-ports the API 31 splash to API 26–30, and sizes the icon for watches. Without it, meeting WO-V15 on every supported version would mean writing our own starting-window activity.

The setup:
- A splash theme `Theme.PillsnerWear.Starting` with parent `Theme.SplashScreen` sets:
  - `windowSplashScreenBackground` to `@android:color/black`
  - `windowSplashScreenAnimatedIcon` to the launcher icon, so the startup icon matches the launcher icon
  - `postSplashScreenTheme` to `@style/Theme.PillsnerWear`
- The activity uses the splash theme in the manifest, and `MainActivity` calls `installSplashScreen()` before `super.onCreate`.
- The library's `windowSplashScreenIconBackgroundColor` is used only if the icon needs a background at the size the library draws on a watch.
- The version is the newest stable release, added to the version catalog through the `dependency-bump` skill. The README Toolchain table is updated in step.

*Alternatives considered:*
- Rely on the platform splash with no library. Rejected because no icon appears below API 31 (proposal critic SPEC-1).
- Raise the watch's minSdk to 31. Rejected because it drops Wear OS 3 watches on API 30 to avoid a small first-party library, and CLAUDE.md forbids changing the minimum SDK to gain a feature without its own proposal.

**Tiles.** The requirement's clause on tiles is forward-looking. The app has no tile today, so there is no scenario for it. A change that adds a tile must add a scenario under this requirement.

**D3. Keep the container and text colours, and prove contrast on black in a test.** The ratios are all well above WCAG AA:

| Foreground | Background | Ratio |
| --- | --- | --- |
| `onSurface` `#E1E3E0` | black | 16.27 : 1 |
| `onSurfaceVariant` `#BFC9C4` | black | 12.37 : 1 |
| `onSecondaryContainer` `#D3E4FF` | `#1D4E7F` | 6.66 : 1 |
| `onErrorContainer` `#FFDAD6` | `#93000A` | 7.24 : 1 |

The cards themselves stand out from black at 2.45 : 1 (`#1D4E7F`) and 2.24 : 1 (`#93000A`). Those ratios are below the 3 : 1 that WCAG sets for non-text elements, but it doesn't apply here. The card edge isn't what the user needs to perceive: every card holds text and an icon, and its meaning is carried by the icon and the TalkBack description, not by the fill. The spec only requires the container to differ from black. *Open for review:* if the critic or a reviewer wants 3 : 1 card edges, lighten the containers in a follow-up change. That would change the phone's colour roles too, so it doesn't belong in this fix.

A unit test computes each ratio from the theme's own values. Red stays reserved for the overdue state, as design system §2.4 requires.

**D4. Pin the result in three layers of tests.**
- A JVM unit test reads the watch colour scheme and asserts that `background` and `surfaceContainerLow` equal `Color.Black`, plus the contrast pairs from D3. To make this possible, the scheme moves from `private` to `internal`.
- An instrumented Compose test captures each screen state (agenda with doses, empty, empty with the footer, details). It asserts `#000000` on every pixel inside the content circle that isn't covered by one of the excluded elements the spec names: cards, text, icons, the app mark, the scroll indicator and the system time. The excluded areas come from the semantics bounds of those nodes, plus the scroll indicator's edge band. This is the same assertion as the spec scenarios, not a looser sample.
- An instrumented test asserts that the activity's resolved `android:windowBackground` is black, that the starting theme's `windowSplashScreenBackground` is black, and that its icon is the launcher icon.
- The startup sequence itself (frames from launch to the agenda) is checked on a Wear emulator below API 31 (API 30, Wear OS 3) and on one at API 31 or later (the CI image, API 37). The library takes different code paths on either side of API 31.

**D5. Add a watch section to the design system: §2.5 "Watch colour".** It says the watch builds from black (`#000000`) for every screen, tile and the startup screen, quotes WO-V13 and WO-V15 with links, and lists the watch's role mapping: the phone's dark roles, except that `background` and `surfaceContainerLow` are black. The same content goes into `docs/design-system.html`. This closes the gap the `pillsner-wear-color-critic` and `pillsner-wear-critic` flag. The `pillsner-designer` agent and the UI skills read the design system, so update their watch mentions if they repeat the old rule.

## Risks / Trade-offs

- [The pixel test is flaky on emulators with a scaled or anti-aliased round mask, or where glyph anti-aliasing bleeds past a text node's bounds] → Inset the content circle by a few pixels from the display edge, pad each excluded element's bounds by 1 dp, and compare exact RGB only outside those areas.
- [The `fix-watch-agenda-bottom-inset` change touches the same screens and the same spec file] → This change only adds a requirement and doesn't edit the layout. If that change archives first, rebase. The delta stays valid because it is ADDED, not MODIFIED.
- [Wear OS draws something non-black at startup that the app doesn't control, such as the launcher's own launch animation before the app's window exists] → The spec counts from the app's first starting-window frame. Task 3.2 checks both API levels. If a frame still fails, revisit D2.
- [No API 30 Wear system image is available locally] → Install `system-images;android-30;android-wear;x86` through sdkmanager. If that's impossible, task 3.2 records the API 30 check as a manual test in the pull request's callout instead of skipping it silently.

## Migration Plan

No data or schema change. Ship it in the next watch release. Once Play accepts the build, reply on issue #119. To roll back, revert the commit, which restores the rejected state.
