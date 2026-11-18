# Tasks

## 1. Black colour roles in the watch theme

- [x] 1.1 Write a failing JVM unit test, `WearThemeTest` under `src/wear/src/test/.../ui/theme/`. It asserts that the watch colour scheme's `background` and `surfaceContainerLow` equal `Color.Black`, and that each D3 text pair meets at least 4.5 : 1 (computed from the scheme's own values). Change the scheme from `private` to `internal` so the test can read it. Verify that the test fails on the current `#101413`.
- [x] 1.2 In `WearTheme.kt`, set `background` and `surfaceContainerLow` to `Color.Black` (design D1). Update the KDoc so it says the watch builds from black and cites WO-V13. Verify that `WearThemeTest` passes and that `grep -rn "101413" src/wear/src/main/kotlin` finds nothing.

## 2. Black window and startup screen

- [x] 2.1 Add `androidx.core:core-splashscreen` at its newest stable version to `src/gradle/libs.versions.toml` and to the wear module's dependencies through the `dependency-bump` skill, and add it to the CONTRIBUTING.md Toolchain table (design D2). Verify that `./gradlew :wear:assembleDebug` succeeds and that the version is the newest stable one on Google's Maven repository.
- [x] 2.2 Write a failing instrumented test, `StartupThemeTest`, that launches the watch activity and asserts three things: the resolved `android:windowBackground` is a colour drawable equal to `#000000`; the manifest's starting theme sets `windowSplashScreenBackground` to `#000000`; and its `windowSplashScreenAnimatedIcon` is the launcher icon. Verify that it fails on the current `deep_moss` theme.
- [x] 2.3 In `src/wear/src/main/res/values/themes.xml`, set `android:windowBackground` to `@android:color/black` and add `Theme.PillsnerWear.Starting` (parent `Theme.SplashScreen`, black background, the launcher icon, `postSplashScreenTheme` = `Theme.PillsnerWear`). Point the activity's `android:theme` at the starting theme, and call `installSplashScreen()` in `MainActivity` before `super.onCreate`. Remove `deep_moss` from `colors.xml` and update that file's comment. Verify that the 2.2 test passes and that `grep -rn deep_moss src/wear` finds nothing.

## 3. Every screen renders on black

- [ ] 3.1 Add an instrumented Compose test, `BlackBackgroundTest` under `src/wear/src/androidTest/.../ui/`, that captures the agenda with a due and an overdue dose, the empty state, the empty state with the connection footer, and the details screen. Capture the agenda both scrolled to the top and scrolled to the end. For each capture, assert `#000000` on every pixel inside the content circle (inset a few pixels from the edge) that isn't covered by one of the excluded elements the spec names: cards, text, icons, the app mark, the scroll indicator and the system time. Take their areas from semantics node bounds padded by 1 dp, plus the scroll indicator's edge band (design D4). Also assert that each card's fill differs from black. Verify that the test fails against a theme with `#101413` restored temporarily, then passes through `pillsner-test-runner` on the round CI emulator profile.
- [ ] 3.2 Run the startup check from the spec's "Startup on black" scenario, through the `adb-device-scenarios` skill, on a Wear emulator at API 30 (install `system-images;android-30;android-wear;x86` if it's missing) and on the API 37 CI image. On each, cold-launch the watch app from the app list after `adb shell am force-stop`, and capture frames from launch until the agenda with `adb shell screenrecord`, then extract frames. Verify on each API level that every frame from the app's first starting-window frame shows only the launcher icon, centred and 48 dp across (measured against the display density), on `#000000`. Record the measured result per API level in the Verification section. If an API level can't be run locally, add it to the pull request's manual testing callout with these steps instead of skipping it. If a frame fails, stop and revisit design D2.
- [ ] 3.3 Run `pillsner-ui-review` on the watch UI and fix what it reports. Verify that the review is clean.

## 4. Design system

- [x] 4.1 Add §2.5 "Watch colour" to `docs/design-system.md` (design D5): the watch builds from black for every screen, tile and the startup screen, citing WO-V13 and WO-V15 with links; the watch role mapping, with `background` and `surfaceContainerLow` set to black; and the contrast table from D3. Mirror it in `docs/design-system.html`. Verify that both files describe the same mapping and that the HTML renders in a browser.
- [ ] 4.2 Update the watch mentions in `.claude/agents/pillsner-designer.md`, the `pillsner-theme`, `pillsner-ui-build` and `pillsner-ui-review` skills, and `.claude/agents/pillsner-wear-critic.md` wherever they say the watch uses the dark surface or that the design system has no watch section. Verify with `grep -rn "Deep Moss\|dark surface\|does not define the watch" .claude` that no watch-specific line still states the old rule.

## 5. Website

- [ ] 5.1 Run the `website-feature-sync` skill for this change. The watch's background colour is visible, but the site describes no watch colours and shows no watch screenshot, so the expected outcome is no page change. Update any page or screenshot it does find in all six locales. Verify that `hugo --minify --panicOnWarning` passes and that every locale has the same structure as English.

## 6. Review and verification

- [ ] 6.1 Run `pillsner-wear-color-critic` and `pillsner-wear-critic` on the branch through the `change-delivery-loop` skill, and record their rows in `.claude/critic-ledger.md`. Verify that both critics are satisfied: no Blocker or Major is open.
- [ ] 6.2 Run the `verify-change` skill and write the Verification section of this file. Verify that the unit tests, lint, the wear debug assembly and the selected instrumented tests are green, with failures quoted verbatim.
