# Proposal

## Why

**GitHub Issue:** #116 (https://github.com/hexmasternl/pillsner/issues/116)

On a round watch, when the agenda is longer than the screen, the last dose can't be scrolled far enough up: its card stops about 40 px above the bottom edge on a small round watch, and the round edge cuts off its corners (the card reaches about 209 px from the centre of a 192 px circle). The text stays inside the circle, but the entry doesn't fit. The `wearable-app` spec already says the list scrolls "to reveal every entry" on a round screen. The app doesn't meet that, and nothing tests the end of the list: `fix-wear-round-edge-clipping` (#107) covered only the empty state, the footer and the top of the list. This is the same Wear quality guideline Play rejected the watch app for before, so it should be fixed before the next Wear release.

## What Changes

- At the end of a long agenda, the last dose card can be scrolled far enough that its whole outline sits inside the circle of a round screen, at the default and at the largest watch font size.
- The details screen's last line gets the same check at the end of its list.
- `RoundScreenFitTest` gets cases that measure the outline of the last card (its shape, not just its text) against the circle once the list is scrolled to the end. Triage already wrote and committed them on `feature/fix-watch-agenda-bottom-inset` (`f56aa53`).
- Nothing changes in what the watch shows, in what order, in which language, or on a square watch beyond spacing.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `wearable-app`: "Wear form factor and accessibility" gets a concrete scenario for the end of the list on a round screen: the last entry lies entirely inside the circle once scrolled to the end, at the default and the largest font size. The existing "Round screen" scenario already implies this; the new scenario makes it testable, the way #107 did for the top.

## Impact

- **Code**: `src/wear/.../ui/UpcomingDosesScreen.kt` and `DoseCard.kt`, and `DoseDetailsScreen.kt` if its end turns out to clip too. Nothing outside the watch module; no phone, `src/shared` or sync change.
- **Tests**: `src/wear/src/androidTest/.../ui/RoundScreenFitTest.kt` (the triage reproduction, already committed). Runs on `Wear_Small_Round_API_36`, and in CI's instrumented job.
- **Strings, dependencies, schema**: none.
- **Website**: none. It's a layout fix with nothing new a user can do.
- **Release**: the watch bundle goes to Play's Wear track with the next release.
