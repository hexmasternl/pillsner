## Why

**GitHub Issue:** #10 (https://github.com/hexmasternl/pillsner/issues/10)

On the schedule editor screen, the three schedule-pattern options ("Every N days", "Weekdays", "Every N hours") are shown as a `SingleChoiceSegmentedButtonRow` of equal-width segments. In several supported languages the first and last labels are long enough to wrap onto a second line while the middle label stays on one, so the row renders with uneven, misaligned segments (reported in [GitHub issue #10](https://github.com/hexmasternl/pillsner/issues/10)). This is a visible polish defect on a screen every user hits when adding or editing a schedule, and it gets worse at larger font scales.

## What Changes

- Fix the schedule-pattern selector in the schedule editor so its three options never wrap unevenly: all segments render at a shared, consistent height regardless of locale or font scale, and no label is truncated.
- Keep the existing single-choice, three-option selection behavior and string resources; this is a layout fix, not a behavior change.
- Add locale/font-scale coverage (screenshot or semantics test) so the fix is verifiable and regressions are caught.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `schedule-editor`: the "Pattern selection" requirement gains an explicit layout/accessibility rule that the three pattern options must render at a uniform height and without misalignment across supported locales and font scales, and without label truncation.

## Impact

- `src/app/src/main/java/nl/hexmaster/pillsner/ui/medicines/schedule/ScheduleEditorScreen.kt` (the `SingleChoiceSegmentedButtonRow` around line 180 and `SchedulePattern.labelRes()` around line 431).
- Existing string resources `schedule_pattern_every_n_days`, `schedule_pattern_weekdays`, `schedule_pattern_every_n_hours` in `values/strings.xml` and the `de`, `es`, `fr`, `nl`, `pt` translations (reviewed for fit, not necessarily reworded).
- Schedule editor Compose UI tests / screenshot tests covering this row.
- No changes to domain logic, persistence, or scheduling.
