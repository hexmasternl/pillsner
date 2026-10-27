# Proposal

## Why

**GitHub Issue:** #107 (https://github.com/hexmasternl/pillsner/issues/107)

Google Play rejected the Wear OS app under the Wear app quality guideline on watch shapes (#107): text is cut off by the edge of a round screen. On a small round watch the empty state ("No medicines scheduled for today or tomorrow") and the footer ("Open Pillsner on your phone to sync") run into the bezel, at the default font size and more so at the largest. That is exactly the screen a reviewer sees, because a review device has no paired phone. The `wearable-app` spec already says no text may be cut off on a round screen; the app does not meet it, and nothing tests it.

The watch's instrumented tests have also not been run on a watch. The only Wear image installed locally is API 37, on which every Compose UI test crashes inside Espresso (`InputManager.getInstance` no longer exists), and no stable androidx.test release fixes that yet. So the round-screen check has to run on a Wear OS image the test stack supports.

## What Changes

- Text that is not a card (the empty state, the phone footer, the time headings on the agenda, and the label-and-value rows on the details screen) gets horizontal and top insets sized for a round screen, as a fraction of the screen width rather than a fixed 8–12 dp, so every line stays inside the circle at the default and the largest watch font size.
- A new instrumented test, `RoundScreenFitTest`, measures every laid-out line of text against the circle of the display, for the screen states that fit without scrolling, at font scale 1.0 and at the largest watch font scale. It is skipped on a square watch.
- The watch's instrumented tests run on a small round Wear OS emulator at API 36. The `emulator-tests` skill records that AVD as the place where `wear` tests run, instead of the phone emulator.
- No change to what the watch shows, in what order, or in which language.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `wearable-app`: "Wear form factor and accessibility" gets concrete scenarios for the round screen: the empty state and the phone footer stay inside the circle, at the default and at the largest font size, and the first lines of both screens clear the bezel at the top. This builds on the wording in `wear-day-overview`, which must be archived first.

## Impact

- **Code**: `src/wear/.../ui/UpcomingDosesScreen.kt`, `DoseDetailsScreen.kt`, `ui/theme/WearDimens.kt`. Nothing outside the watch module.
- **Tests**: new `src/wear/src/androidTest/.../ui/RoundScreenFitTest.kt` (already on `feature/fix-wear-round-edge-clipping` as the reproduction from triage). The existing watch tests also run on the new AVD.
- **Tooling**: the Android SDK command-line tools and the Wear OS API 36 system image are installed locally to create the AVD. No Gradle dependency changes.
- **Release**: the watch bundle has to be rebuilt and resubmitted to Play's Wear track for the review to pass.
- **Not in scope**: running watch tests in CI (that belongs with `ci-instrumented-tests`), and the Espresso crash on API 37 (it waits for a stable androidx.test release and gets its own issue).
- **Issue**: #107.
