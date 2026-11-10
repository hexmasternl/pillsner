# Tasks

## 1. The failing tests

- [x] 1.1 Work in the existing worktree `../pillsner-worktrees/fix-watch-agenda-bottom-inset` on `feature/fix-watch-agenda-bottom-inset`, which triage created with the failing tests (`f56aa53`). Merge `development` into it so it carries this change's artifacts, and verify `git log` shows `f56aa53` and the proposal commits.
- [x] 1.2 Run `RoundScreenFitTest` on `Wear_Small_Round_API_36` against the unchanged screen code, and verify `theLastDoseOfALongAgendaClearsTheBezel` and `theLastDoseOfALongAgendaClearsTheBezelAtTheLargestFont` fail with "A dose card crosses the edge of the round screen at the end of the list", while the other 11 cases pass. *Evidence (test runner, `Wear_Small_Round_API_36`, HEAD `85c53c1`): 13 cases, 11 pass; both fail with that message. The card at Rect(20.5, 248.0, 363.5, 343.7) reaches 209.29 px from the centre of a 192 px circle, 208.19 px at the largest font.*
- [ ] 1.3 Add a case for the end of the details screen: with font scale 1.24 and made-up details long enough to scroll, check that the last line, measured at the end of the list, lies inside the circle (design D9). Record whether it fails on the unchanged code.
- [ ] 1.4 Change how the two triage cases find the last dose card (design D5): the click-action node with the greatest bottom edge once scrolled to the end, instead of the click-action nodes in the lower half. Keep the outline assertion as it is. Verify both still fail on the unchanged screen code with the same message.
- [ ] 1.5 Make every scroll-to-end in `RoundScreenFitTest` go to the column's last item rather than a fixed index (design D6), including `everyLanguageFitsTheRoundScreenAtTheLargestFont`, and verify the cases that passed in 1.2 still pass.
- [ ] 1.6 Add a failing packaging test (design D7): the never-synced footer rendered for a Portuguese and for a German locale is not the English text. Verify it fails on the unchanged build file.
- [ ] 1.7 Add failing tests for the mark and version: in `UpcomingDosesScreenTest`, the agenda's last item shows "Version 1.2.3" for a made-up version name `1.2.3`, with doses, in the empty state, and after the connection footer when the phone is not connected; it has no click action; a screen reader gets the version text and no separate node for the mark; and the details screen shows neither. In `RoundScreenFitTest`, scrolled to the end, the mark's bounding box and every line of the version text lie inside the circle in each state, font scale and language of the table in design D6. Verify they fail because the item does not exist.

## 2. The mark, the version and the end of the list

- [ ] 2.0 Package all six languages in the watch app (design D7): `resourceConfigurations` in `src/wear/build.gradle.kts` lists en, nl, de, fr, es and pt, with its comment corrected. Verify the 1.6 test passes and the existing six-language case still passes; if a translation now proves too long for the round screen, fix it through the `pillsner-translator` agent.
- [ ] 2.1 Add the last item (design D1–D4): copy `ic_pillsner_logo.xml` and any brand colours it needs into the watch module, add `WearDimens.markSize`, add the `app_version` string in English, turn on `buildFeatures.buildConfig` for the watch, pass `BuildConfig.VERSION_NAME` from `MainActivity` into `UpcomingDosesScreen`, and render the mark above the version line, centred, as the column's last item with the scaffold's end-of-list padding. Verify the 1.7 tests pass in English and that every existing at-rest case ("Empty state on a round screen", "Footer on a round screen") still passes; if one does not, apply design D8. Record in design.md whether the never-synced empty state shows the mark and version without scrolling at the default font.
- [ ] 2.2 Run the two end-of-list cases from 1.4 and verify they pass at font scale 1.0 and 1.24. If they don't, give dose rows `minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding)`, then the D4 fallback, and record what was needed in design.md.
- [ ] 2.3 If 1.3 failed, fix the end of the details screen the same way, and verify its case passes. Otherwise mark it not needed.
- [ ] 2.4 Translate `app_version` into nl, fr, es, pt and de through the `pillsner-translator` agent (`app-strings` skill), and verify lint reports no `MissingTranslation` or `ExtraTranslation` and the six-language cases of 1.7 pass.
- [ ] 2.5 Add a `WearDevices.SMALL_ROUND` preview of an agenda long enough to scroll, make sure every existing preview passes a version name, check the square preview for any extra gap at the end, and run the `pillsner-ui-review` skill on the watch screens with no violations left.

## 3. Website

- [ ] 3.1 Run the `website-feature-sync` skill for this change: update the watch page in all six locales if it describes or pictures the end of the agenda, otherwise record that nothing on the site needed changing. Verify `hugo --minify --panicOnWarning` passes and every locale has the same structure as English.

## 4. Verification

- [ ] 4.1 Run the full watch instrumented suite on `Wear_Small_Round_API_36` through the `pillsner-test-runner` agent, and verify every `RoundScreenFitTest` and `UpcomingDosesScreenTest` case passes and nothing that passed before fails. The known CI-only #105 timeout (`aNewListArrivesWhileTheScreenIsOpen`) is reported separately, not counted as a regression.
- [ ] 4.2 Take before and after screenshots of the agenda scrolled to the end on the round emulator, at font scale 1.0 and 1.24, and one of the empty state, for the pull request.
- [ ] 4.3 Run the `verify-change` skill (unit tests, lint, assembly) and record the result in the `## Verification` section.
- [ ] 4.4 Open the pull request into `development` with `Fixes #116`.
