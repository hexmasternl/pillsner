# Design

## Context

The watch theme (`ui/theme/WearTheme.kt`) copies the dark column of `docs/design-system.md` §2.2. It maps the design system's dark `background` and `surfaceContainerLow` to Deep Moss `#101413`. Both screens wrap their content in Wear Compose Material 3's `AppScaffold` and `ScreenScaffold` (`wearCompose` 1.6.2), which fill with `MaterialTheme.colorScheme.background` and set no colour of their own. The window theme `Theme.PillsnerWear` paints `@color/deep_moss` as `android:windowBackground`. The app has no splash library, no tiles and no complications. Dose cards fill with `secondaryContainer` (`#1D4E7F`) or `errorContainer` (`#93000A`). Secondary text uses `onSurfaceVariant` (`#BFC9C4`).

## Goals / Non-Goals

**Goals:**
- Make black the only background colour the watch can show behind its content, enforced by tests.
- Write the watch's colour rules into the design system.

**Non-Goals:**
- Changing the phone's dark theme. The phone keeps Deep Moss, because the tinted background is right for phones.
- Retuning the watch's accent or container colours. They already meet contrast on black (D3).
- Adding a tile or the AndroidX splash-screen library.
- Changing the launcher icon. Its green background is the icon's own layer, not the screen background.

## Decisions

**D1. Set `background` and `surfaceContainerLow` to `Color.Black` in the watch palette.** Wear Material 3's baseline dark scheme builds from black: its `background` is black, and so are the lowest surface roles. `surfaceContainerLow` is set to black too, because any component that defaults to it would otherwise paint a tinted panel that looks like background. `surfaceContainer` and `surfaceContainerHigh` keep their tones so that raised elements still separate from the canvas.
*Alternative considered:* override `containerColor` on each `AppScaffold` and `ScreenScaffold`. Rejected because it fixes two call sites but leaves the next screen, dialog or confirmation on the tinted colour. Fixing it in the theme covers them all.

**D2. Replace `deep_moss` in the watch's `colors.xml` with `@android:color/black` for `android:windowBackground`.** Remove the `deep_moss` entry once nothing references it. The window background is what the system shows at startup, before Compose draws. With no splash library, Wear OS shows the launcher icon on the window background, so a black window satisfies WO-V15 without a new dependency (CLAUDE.md asks for no dependency without a reason).
*Alternative considered:* add `androidx.core:core-splashscreen` with `windowSplashScreenBackground` set to black. Rejected because it adds a dependency for behaviour the platform already gives. If the startup check in task 3.2 shows a non-black frame or a different icon, revisit this decision and update the design.

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
- An instrumented Compose test captures each screen state (agenda with doses, empty, empty with the footer, details) and samples background pixels, such as the corners inside the circle and the gap between cards, asserting `#000000`.
- An instrumented test asserts that the activity's resolved `android:windowBackground` is black.

**D5. Add a watch section to the design system: §2.5 "Watch colour".** It says the watch builds from black (`#000000`) for every screen, tile and the startup screen, quotes WO-V13 and WO-V15 with links, and lists the watch's role mapping: the phone's dark roles, except that `background` and `surfaceContainerLow` are black. The same content goes into `docs/design-system.html`. This closes the gap the `pillsner-wear-color-critic` and `pillsner-wear-critic` flag. The `pillsner-designer` agent and the UI skills read the design system, so update their watch mentions if they repeat the old rule.

## Risks / Trade-offs

- [The pixel-sampling test is flaky on emulators with a scaled or anti-aliased round mask] → Sample points well inside the content circle and away from text. Compare exact RGB on opaque areas only.
- [The `fix-watch-agenda-bottom-inset` change touches the same screens and the same spec file] → This change only adds a requirement and doesn't edit the layout. If that change archives first, rebase. The delta stays valid because it is ADDED, not MODIFIED.
- [Wear OS draws something non-black at startup that the app doesn't control] → Task 3.2 checks startup on the emulator. If it fails, revisit D2.

## Migration Plan

No data or schema change. Ship it in the next watch release. Once Play accepts the build, reply on issue #119. To roll back, revert the commit, which restores the rejected state.
