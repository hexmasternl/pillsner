# Tasks

## 1. The failing tests

- [ ] 1.1 Work in the existing worktree `../pillsner-worktrees/fix-watch-agenda-bottom-inset` on `feature/fix-watch-agenda-bottom-inset`, which triage created with the failing tests (`f56aa53`). Merge `development` into it so it carries this change's artifacts, and verify `git log` shows `f56aa53` and the proposal commit.
- [ ] 1.2 Run `RoundScreenFitTest` on `Wear_Small_Round_API_36` against the unchanged screen code, and verify `theLastDoseOfALongAgendaClearsTheBezel` and `theLastDoseOfALongAgendaClearsTheBezelAtTheLargestFont` fail with "A dose card crosses the edge of the round screen at the end of the list", while the other 11 cases pass.
- [ ] 1.3 Add a case for the end of the details screen: with font scale 1.24 and made-up details long enough to scroll, check that the last line, measured at the end of the list, lies inside the circle (design D3). Record whether it fails on the unchanged code.

## 2. The fix

- [ ] 2.1 Give each dose row on the agenda `minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding)` (design D1), and verify both end-of-list cases from 1.2 pass at font scale 1.0 and 1.24. If they don't, apply the D1 fallback and record it in design.md.
- [ ] 2.2 If 1.3 failed, fix the end of the details screen the same way, and verify its case passes. Otherwise mark it not needed.
- [ ] 2.3 Add a `WearDevices.SMALL_ROUND` preview of an agenda long enough to scroll, check the square preview for any extra gap at the end, and run the `pillsner-ui-review` skill on the watch screens with no violations left.

## 3. Verification

- [ ] 3.1 Run the full watch instrumented suite on `Wear_Small_Round_API_36` through the `pillsner-test-runner` agent, and verify every `RoundScreenFitTest` case passes and nothing that passed before fails. The known CI-only #105 timeout (`aNewListArrivesWhileTheScreenIsOpen`) is reported separately, not counted as a regression.
- [ ] 3.2 Take before and after screenshots of the agenda scrolled to the end on the round emulator, at font scale 1.0 and 1.24, for the pull request.
- [ ] 3.3 Run the `verify-change` skill (unit tests, lint, assembly) and record the result in the `## Verification` section.
- [ ] 3.4 Open the pull request into `development` with `Fixes #116`.
