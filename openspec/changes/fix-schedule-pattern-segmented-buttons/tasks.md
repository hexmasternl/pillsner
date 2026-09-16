## 1. Layout fix

- [ ] 1.1 In `ScheduleEditorScreen.kt`, wrap the pattern `SingleChoiceSegmentedButtonRow` with `Modifier.height(IntrinsicSize.Min)` and give each `SegmentedButton` `Modifier.fillMaxHeight()`, so all three segments share one row height.
- [ ] 1.2 Set each pattern label `Text` to `textAlign = TextAlign.Center`, `maxLines = 2`, `softWrap = true`, and no ellipsis/truncation overflow.

## 2. Verification

- [ ] 2.1 Manually verify the pattern row in the app (or previews) for `en`, `de`, `es`, `fr`, `nl`, `pt` locales, confirming all three segments render at the same height whether or not a label wraps.
- [ ] 2.2 Manually verify the pattern row at the largest system font scale, confirming the editor still scrolls fully and no label is truncated.
- [ ] 2.3 Add or update a Compose UI test (screenshot or semantics-based) for the schedule editor that asserts the three pattern segments have equal height/bounds, covering at least one locale where a label wraps (e.g. `fr` or `nl`).
- [ ] 2.4 Run `pillsner-ui-review` on the changed composable and resolve any findings.

## 3. Build and checks

- [ ] 3.1 Run the unit test task from the `src` Gradle project root.
- [ ] 3.2 Run the lint task from the `src` Gradle project root.
- [ ] 3.3 Run the new/updated Compose UI test and confirm it passes.
