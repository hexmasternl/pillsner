# Tasks

## 1. Black colour roles in the watch theme

- [x] 1.1 Write a failing JVM unit test, `WearThemeTest` under `src/wear/src/test/.../ui/theme/`. It asserts that the watch colour scheme's `background` and `surfaceContainerLow` equal `Color.Black`, and that each D3 text pair meets at least 4.5 : 1 (computed from the scheme's own values). Change the scheme from `private` to `internal` so the test can read it. Verify that the test fails on the current `#101413`.
- [x] 1.2 In `WearTheme.kt`, set `background` and `surfaceContainerLow` to `Color.Black` (design D1). Update the KDoc so it says the watch builds from black and cites WO-V13. Verify that `WearThemeTest` passes and that `grep -rn "101413" src/wear/src/main/kotlin` finds nothing.

## 2. Black window and startup screen

- [x] 2.1 Add `androidx.core:core-splashscreen` at its newest stable version to `src/gradle/libs.versions.toml` and to the wear module's dependencies through the `dependency-bump` skill, and add it to the CONTRIBUTING.md Toolchain table (design D2). Verify that `./gradlew :wear:assembleDebug` succeeds and that the version is the newest stable one on Google's Maven repository.
- [x] 2.2 Write a failing instrumented test, `StartupThemeTest`, that launches the watch activity and asserts three things: the resolved `android:windowBackground` is a colour drawable equal to `#000000`; the manifest's starting theme sets `windowSplashScreenBackground` to `#000000`; and its `windowSplashScreenAnimatedIcon` is the launcher icon. Verify that it fails on the current `deep_moss` theme.
- [x] 2.3 In `src/wear/src/main/res/values/themes.xml`, set `android:windowBackground` to `@android:color/black` and add `Theme.PillsnerWear.Starting` (parent `Theme.SplashScreen`, black background, the launcher icon, `postSplashScreenTheme` = `Theme.PillsnerWear`). Point the activity's `android:theme` at the starting theme, and call `installSplashScreen()` in `MainActivity` before `super.onCreate`. Remove `deep_moss` from `colors.xml` and update that file's comment. Verify that the 2.2 test passes and that `grep -rn deep_moss src/wear` finds nothing.
- [x] 2.4 Correction from task 3.2 (design D2, "Correction found during implementation"): add `src/wear/src/main/res/drawable/splash_icon.xml`, the launcher's green background disc plus its foreground artwork as one flat drawable, inset so the visible icon is 48 dp inside the library's 60 dp slot. Point `windowSplashScreenAnimatedIcon` at it, and change `StartupThemeTest`'s icon assertion to `R.drawable.splash_icon` first, watching it fail. Verify that `StartupThemeTest` passes and that the drawable reuses the launcher's colour resources and paths rather than copying hex values.

## 3. Every screen renders on black

- [x] 3.1 Add an instrumented Compose test, `BlackBackgroundTest` under `src/wear/src/androidTest/.../ui/`, that captures the agenda with a due and an overdue dose, the empty state, the empty state with the connection footer, and the details screen. Capture the agenda both scrolled to the top and scrolled to the end. For each capture, assert `#000000` on every pixel inside the content circle (inset a few pixels from the edge) that isn't covered by one of the excluded elements the spec names: cards, text, icons, the app mark, the scroll indicator and the system time. Take their areas from semantics node bounds padded by 1 dp, plus the scroll indicator's edge band (design D4). Also assert that each card's fill differs from black. Verify that the test fails against a theme with `#101413` restored temporarily, then passes through `pillsner-test-runner` on the round CI emulator profile.
- [x] 3.2 Run the startup check from the spec's "Startup on black" scenario, through the `adb-device-scenarios` skill, on a Wear emulator at API 30 (install `system-images;android-30;android-wear;x86` if it's missing) and on the API 37 CI image. On each, cold-launch the watch app from the app list after `adb shell am force-stop`, and capture frames from launch until the agenda with `adb shell screenrecord`, then extract frames. Verify on each API level that every frame from the end of the system's window-open transition until the agenda shows only the launcher icon artwork (green ring included), centred and 48 dp across (measured against the display density), on `#000000`. Confirm with animations off that any frame before that point which isn't black comes from the system transition. Record the measured result per API level in the Verification section. If an API level can't be run locally, add it to the pull request's manual testing callout with these steps instead of skipping it. If a frame fails, stop and revisit design D2.
- [x] 3.3 Run `pillsner-ui-review` on the watch UI and fix what it reports. Verify that the review is clean.

## 4. Design system

- [x] 4.1 Add §2.5 "Watch colour" to `docs/design-system.md` (design D5): the watch builds from black for every screen, tile and the startup screen, citing WO-V13 and WO-V15 with links; the watch role mapping, with `background` and `surfaceContainerLow` set to black; and the contrast table from D3. Mirror it in `docs/design-system.html`. Verify that both files describe the same mapping and that the HTML renders in a browser.
- [x] 4.2 Update the watch mentions in `.claude/agents/pillsner-designer.md`, the `pillsner-theme`, `pillsner-ui-build` and `pillsner-ui-review` skills, and `.claude/agents/pillsner-wear-critic.md` wherever they say the watch uses the dark surface or that the design system has no watch section. Verify with `grep -rn "Deep Moss\|dark surface\|does not define the watch" .claude` that no watch-specific line still states the old rule.

## 5. Website

- [x] 5.1 Run the `website-feature-sync` skill for this change. The watch's background colour is visible, but the site describes no watch colours and shows no watch screenshot, so the expected outcome is no page change. Update any page or screenshot it does find in all six locales. Verify that `hugo --minify --panicOnWarning` passes and that every locale has the same structure as English.

## 6. Review and verification

- [ ] 6.1 Run `pillsner-wear-color-critic` and `pillsner-wear-critic` on the branch through the `change-delivery-loop` skill, and record their rows in `.claude/critic-ledger.md`. Verify that both critics are satisfied: no Blocker or Major is open.
- [ ] 6.2 Run the `verify-change` skill and write the Verification section of this file. Verify that the unit tests, lint, the wear debug assembly and the selected instrumented tests are green, with failures quoted verbatim.

> 5.1 outcome (2026-11-18): no website change needed. No page in any locale describes the watch's colours or shows a watch screenshot; the FAQ's "The watch app keeps its own look" stays accurate. `hugo --minify --panicOnWarning` passes, and all six locales have the same 9 content files as English.

> 3.2 outcome (2026-11-18, HEAD 738a734, measured by `pillsner-test-runner` from cold launches on round 384x384 emulators at density 320). First run at 8167738 failed: the icon was 60 dp on both levels, with no green ring on API 30. That led to design D2's correction and task 2.4. Second run:
> - **API 30** (`android-30;android-wear;x86`): the steady startup frame is `#000000` everywhere outside the icon. The icon is 96 px = 48 dp across, centred at (192, 192), with ring/diameter 0.083 (launcher icon 0.083).
> - **API 37** (CI image `android-37.0;android-wear-signed;x86_64`): the steady startup frame is `#000000` everywhere outside the icon. The icon is 96 px = 48 dp across, centred at (192, 192), with ring/diameter 0.083 (launcher icon 0.085).
> - **Before the steady frame:** the only non-black frames are the system's window-open transition (a grey scrim on API 30, the outgoing watch face on API 37). With animations off, both levels go straight from the watch face to the steady frame.

> 3.3 outcome (2026-11-18): `pillsner-ui-review` on `git diff development...HEAD -- src/wear` is compliant. There are no colour literals outside `ui/theme` and `res/values/colors.xml` (§2.4), no dynamic colour (§1.5), and the only error-role use is `DoseCard`'s overdue container, which §2.4 allows. The diff adds no composable and no string. Checked: colour, type, layout, components, accessibility, strings.
