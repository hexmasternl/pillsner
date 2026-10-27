# Tasks

## 1. A watch emulator the tests can run on

- [ ] 1.1 Moved to 1.6: `wear-day-overview`'s last open task needs the emulator this group sets up, so it is archived after 1.5 instead of before. Verify 1.6 is done before group 2 starts
- [x] 1.2 Install the Android SDK command-line tools and the Wear OS API 36 system image (through Android Studio's SDK Manager if `sdkmanager` can't fetch it), and verify `sdkmanager --list_installed` shows both. Done: cmdline-tools 23.0.0 and `system-images;android-36;android-wear-signed;x86_64` (Wear OS 6.0). On Windows the `.bat` wrapper splits the package path at `;`, so call it through `cmd /c` with the full path and the package in double quotes
- [x] 1.3 Create the `Wear_Small_Round_API_36` AVD (device `wearos_small_round`) with `avdmanager`, delete the hand-written `Wear_Small_Round` API 37 AVD from triage, and verify `emulator -list-avds` shows only the new watch AVD next to the phone's. Done: `avdmanager` writes `hw.lcd.circular=false` for `wearos_small_round`, so it is set to `true` by hand. A `Wear_OS_Small_Round` AVD created outside this change is left alone
- [x] 1.4 Boot it headless and run the existing watch suite (`:wear:connectedDebugAndroidTest`), and verify the `UpcomingDosesScreenTest` and `DoseDetailsScreenTest` results no longer contain the `InputManager.getInstance` crash; record any other failures verbatim, since #104 and #105 cover known ones. Done: no `InputManager.getInstance` crash on API 36. 19 tests, 12 pass. The 3 `RoundScreenFitTest` cases fail on their own assertion ("Text crosses the edge of the round screen", the reproduction). 4 fail because content below the fold of the small round screen is "not displayed" (#104): `UpcomingDosesScreenTest#theAgendaShowsTodayAndTomorrow` ("Tomorrow"), `#dosesDueAtTheSameTimeShareOneTimeHeading` (node count), `DoseDetailsScreenTest#theDetailsAreReadInThePhoneAppsLanguage` ("Op voorraad"), `#theMedicineBehindTheDoseIsShown` ("24 tablets" when run alone; "No compose hierarchies found" on the first full run)
- [x] 1.5 Update the `emulator-tests` skill: `wear` tests run on `Wear_Small_Round_API_36`, booted the same way as the phone emulator, with the API 37 Espresso crash noted as the reason; verify the skill's commands run as written. Done: the install, create and round-screen check commands in the skill are the ones that ran for 1.2–1.4
- [ ] 1.6 Finish `wear-day-overview`'s open verification tasks (6.1 unit tests and lint, 6.2 watch instrumented tests on the new AVD, 6.3 design-system check) and archive it on `development`, since its delta to "Wear form factor and accessibility" is the base for this one. Verify `openspec list` no longer shows it

## 2. The failing test

- [ ] 2.1 In the `feature/fix-wear-round-edge-clipping` worktree, extend `RoundScreenFitTest` (D4) with font scale 1.24 for the three empty and footer states, and with "scrolled to the top" cases for the agenda (first heading) and the details screen (title), using made-up medicine names
- [ ] 2.2 Run `RoundScreenFitTest` on `Wear_Small_Round_API_36` against the unchanged screen code, and verify it fails with the assertion message "Text crosses the edge of the round screen", not a crash; paste the failing lines into this task's commit message

## 3. The fix

- [ ] 3.1 Add `textInsetFraction` to `WearDimens` and a helper that turns it into dp from the screen width (D2), and verify `:wear:testDebugUnitTest` still passes, including `EdgeToEdgeContractTest`
- [ ] 3.2 Centre the empty state vertically and give it and the footer the round-screen inset (D1, D2), and verify the empty and footer cases of `RoundScreenFitTest` pass at both font scales
- [ ] 3.3 Give the time headings and the detail rows the round-screen inset, and the first item of each screen `ListHeaderDefaults.minimumTopListContentPadding` (D2, D3), and verify the "scrolled to the top" cases pass
- [ ] 3.4 Tune `textInsetFraction` only as far as the test needs, and verify the German and Portuguese empty and footer strings fit by previewing them on `WearDevices.SMALL_ROUND`
- [ ] 3.5 Add `WearDevices.LARGE_ROUND` and `SMALL_ROUND` previews for the empty and footer states, and run the `pillsner-ui-review` skill on the watch screens with no violations left

## 4. Verification

- [ ] 4.1 Run the full watch instrumented suite on `Wear_Small_Round_API_36` through the `emulator-tests` skill, and verify `RoundScreenFitTest` passes and nothing that passed in 1.4 now fails
- [ ] 4.2 Take screenshots of the empty state, the never-synced state and the agenda on the round emulator at font scale 1.0 and 1.24, and attach them to the pull request as the before/after evidence for Play's review
- [ ] 4.3 Run the `verify-change` skill (unit tests, lint, assembly) and record its result in the `## Verification` section
- [ ] 4.4 Open the pull request into `development` with `Fixes #107`, and note in it that the watch bundle must be resubmitted on Play's Wear track after release
