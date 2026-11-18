# Proposal

## Why

**GitHub Issue:** #119 (https://github.com/hexmasternl/pillsner/issues/119)

Google Play rejected the Wear OS app: "Your app does not use a black background for all apps and tiles." The [Wear OS app quality guidelines](https://developer.android.com/docs/quality-guidelines/wear-app-quality) require a black background for every app screen and tile (**WO-V13**) and the app icon on a black background at startup (**WO-V15**). The [Wear colour guidance](https://developer.android.com/design/ui/wear/guides/styles/color) adds that "watches are designed with a black background, instead of the tinted background that phone devices use". The watch reuses the phone's tinted dark background, Deep Moss `#101413`, so it can't ship until this is fixed.

## What Changes

- Every watch screen (the agenda, the dose details, the empty state and the phone-unreachable state) gets a pure black `#000000` background instead of Deep Moss.
- The window behind the first frame and the startup screen become black and show the launcher icon, as WO-V15 requires.
- Dose cards keep their container colours (blue for due, red container for overdue), which already stand out on black. Text keeps at least WCAG AA contrast against black.
- Tests pin the black background: the watch colour scheme, the window theme and each screen's rendered background.
- `docs/design-system.md` and `docs/design-system.html` gain a watch section. It states that the watch builds from black and lists the watch's colour roles, so the phone's dark background is not reused on the watch again.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `wearable-app`: adds a requirement that every watch screen, the startup screen and any future tile use a black background.

## Impact

- `src/wear/src/main/kotlin/nl/hexmaster/pillsner/wear/ui/theme/WearTheme.kt`: the `background` and `surfaceContainerLow` colour roles.
- `src/wear/src/main/res/values/colors.xml` and `themes.xml`: the window background.
- New unit and instrumented tests under `src/wear/src/test` and `src/wear/src/androidTest`.
- `docs/design-system.md` and `docs/design-system.html`: the new watch section.
- The phone app, the sync contract, strings, permissions and dependencies don't change.
- The overlapping in-flight change `fix-watch-agenda-bottom-inset` modifies the "Wear form factor and accessibility" requirement. This change adds a separate requirement rather than modifying that one, so the two don't conflict.
