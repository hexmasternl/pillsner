# Tasks

## 1. Black colour roles in the watch theme

- [ ] 1.1 Write a failing JVM unit test, `WearThemeTest` under `src/wear/src/test/.../ui/theme/`. It asserts that the watch colour scheme's `background` and `surfaceContainerLow` equal `Color.Black`, and that each D3 text pair meets at least 4.5 : 1 (computed from the scheme's own values). Change the scheme from `private` to `internal` so the test can read it. Verify that the test fails on the current `#101413`.
- [ ] 1.2 In `WearTheme.kt`, set `background` and `surfaceContainerLow` to `Color.Black` (design D1). Update the KDoc so it says the watch builds from black and cites WO-V13. Verify that `WearThemeTest` passes and that `grep -rn "101413" src/wear/src/main/kotlin` finds nothing.

## 2. Black window and startup screen

- [ ] 2.1 Write a failing instrumented test that launches the watch activity and asserts that its resolved `android:windowBackground` is a colour drawable equal to `#000000`. Verify that it fails on the current `deep_moss`.
- [ ] 2.2 In `src/wear/src/main/res/values/themes.xml`, set `android:windowBackground` to `@android:color/black`. Remove `deep_moss` from `colors.xml` and update that file's comment (design D2). Verify that the 2.1 test passes and that `grep -rn deep_moss src/wear` finds nothing.

## 3. Every screen renders on black

- [ ] 3.1 Add an instrumented Compose test, `BlackBackgroundTest` under `src/wear/src/androidTest/.../ui/`, that captures the agenda with a due and an overdue dose, the empty state, the empty state with the connection footer, and the details screen. For each, sample background pixels inside the content circle and away from text, such as the gap between cards, and assert `#000000`. Also assert that each card's fill differs from black. Verify that the test passes through `pillsner-test-runner` on a round and a square emulator profile.
- [ ] 3.2 Run the startup check from the spec's "Startup on black" scenario on the emulator: cold-launch the watch app from the app list and capture frames from launch until the agenda (screen record or repeated `adb exec-out screencap`). Verify that every frame is black apart from the launcher icon, and record the result in the Verification section. If any frame isn't black, stop and revisit design D2 before going on.
- [ ] 3.3 Run `pillsner-ui-review` on the watch UI and fix what it reports. Verify that the review is clean.

## 4. Design system

- [ ] 4.1 Add §2.5 "Watch colour" to `docs/design-system.md` (design D5): the watch builds from black for every screen, tile and the startup screen, citing WO-V13 and WO-V15 with links; the watch role mapping, with `background` and `surfaceContainerLow` set to black; and the contrast table from D3. Mirror it in `docs/design-system.html`. Verify that both files describe the same mapping and that the HTML renders in a browser.
- [ ] 4.2 Update the watch mentions in `.claude/agents/pillsner-designer.md`, the `pillsner-theme`, `pillsner-ui-build` and `pillsner-ui-review` skills, and `.claude/agents/pillsner-wear-critic.md` wherever they say the watch uses the dark surface or that the design system has no watch section. Verify with `grep -rn "Deep Moss\|dark surface\|does not define the watch" .claude` that no watch-specific line still states the old rule.

## 5. Website

- [ ] 5.1 Run the `website-feature-sync` skill for this change. The watch's background colour is visible, but the site describes no watch colours and shows no watch screenshot, so the expected outcome is no page change. Update any page or screenshot it does find in all six locales. Verify that `hugo --minify --panicOnWarning` passes and that every locale has the same structure as English.

## 6. Review and verification

- [ ] 6.1 Run `pillsner-wear-color-critic` and `pillsner-wear-critic` on the branch through the `change-delivery-loop` skill, and record their rows in `.claude/critic-ledger.md`. Verify that both critics are satisfied: no Blocker or Major is open.
- [ ] 6.2 Run the `verify-change` skill and write the Verification section of this file. Verify that the unit tests, lint, the wear debug assembly and the selected instrumented tests are green, with failures quoted verbatim.
