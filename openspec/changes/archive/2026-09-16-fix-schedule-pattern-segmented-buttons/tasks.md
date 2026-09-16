## 1. Layout fix

- [x] 1.1 In `ScheduleEditorScreen.kt`, wrap the pattern `SingleChoiceSegmentedButtonRow` with `Modifier.height(IntrinsicSize.Min)` and give each `SegmentedButton` `Modifier.fillMaxHeight()`, so all three segments share one row height.
- [x] 1.2 Set each pattern label `Text` to `textAlign = TextAlign.Center` with no `maxLines` cap and no ellipsis/truncation overflow, so a label can wrap onto as many lines as it needs (corrected from an initial `maxLines = 2` cap after 2.2 found it clipped a label at the largest font scale).

## 2. Verification

- [x] 2.1 Manually verify the pattern row in the app (or previews) for the two currently-shipped locales, `en` and `nl` (the app's `resourceConfigurations` packages no other language today), confirming all three segments render at the same height whether or not a label wraps. Verified on the Pixel_9 emulator with real screenshots: English (all one line) and Dutch (outer two wrap, "Vaste dagen" stays on one line, all three share one height).
- [x] 2.2 Manually verify the pattern row at the largest system font scale, confirming the editor still scrolls fully and no label is truncated. Verified on-device at 2x font scale in Dutch: this caught the initial `maxLines = 2` implementation silently clipping "Om de zoveel uur" to "Om de zoveel" (see 1.2 and design.md); removing the line cap fixed it — "uur" now renders on a third line and nothing is clipped.
- [x] 2.3 Add or update a Compose UI test (screenshot or semantics-based) for the schedule editor that asserts the three pattern segments have equal height/bounds, covering `en` (fits on one line) and `nl` (a label wraps) — the two locales actually packaged by `resourceConfigurations`.
- [x] 2.4 Run `pillsner-ui-review` on the changed composable and resolve any findings.

## 3. Build and checks

- [x] 3.1 Run the unit test task from the `src` Gradle project root.
- [x] 3.2 Run the lint task from the `src` Gradle project root.
- [x] 3.3 Run the new/updated Compose UI test and confirm it passes. Ran on the Pixel_9 emulator via `adb shell am instrument`: 2/2 tests pass (English and Dutch).
