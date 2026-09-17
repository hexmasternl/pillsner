## 1. Shared bucketing extraction

- [x] 1.1 Extract the bucket-start and bucket-index logic from `SummariseUsageHistory` into a new internal `domain/history/UsageBucketing.kt` (`bucketStarts(firstDay, today, byWeek, firstDayOfWeek)`, `bucketIndex(starts, day)`), with no behaviour change.
- [x] 1.2 Update `SummariseUsageHistory` to call the extracted functions instead of its private ones.
- [x] 1.3 Run `SummariseUsageHistoryTest` and confirm every existing case still passes unchanged.

## 2. Domain model and calculation

- [x] 2.1 Add `TimeDeviationBucket` and `TimeDeviationHistory` to `domain/model/TimeDeviation.kt` (`averageMinutes: Int?` null exactly when `takenCount == 0`).
- [x] 2.2 Add `SummariseTimeDeviation(clock, firstDayOfWeek)` in `domain/history/SummariseTimeDeviation.kt`, using `UsageBucketing` with `byWeek = period == UsagePeriod.THREE_MONTHS`, computing each taken dose's deviation as `round(abs(recordedAt - scheduledAt) in minutes)` and each bucket's/the period's average as the mean of its taken doses' deviations, rounded to the nearest minute.
- [x] 2.3 Write `SummariseTimeDeviationTest` covering: taken-late and taken-early deviation values from the proposal's own examples (13 minutes, 7 minutes), a bucket average across several taken doses, a bucket with only skipped/missed doses (no average), one-week and one-month periods bucketed daily, a three-month period bucketed weekly with a mid-week window start, and a period with no taken dose anywhere (`averageMinutes == null` at the period level).

## 3. ViewModel and UI state

- [x] 3.1 Inject `SummariseTimeDeviation` into `MedicineHistoryViewModel` and compute a `TimeDeviationHistory` from the same `doses` flow already combined for `SummariseUsageHistory`, keyed to the same `period`.
- [x] 3.2 Add `timeDeviation: TimeDeviationHistory?` to `MedicineHistoryUiState`.
- [x] 3.3 Wire `SummariseTimeDeviation` into whatever constructs `MedicineHistoryViewModel` (DI/factory), alongside the existing `SummariseUsageHistory` wiring.
- [x] 3.4 Extend `MedicineHistoryViewModelTest` to cover the new state field, including the case where `timeDeviation.averageMinutes` is null.

## 4. Timing accuracy chart UI

- [x] 4.1 Add string resources: chart header ("per day" / "per week" for this chart), the overall figure's "less is better" caption, and the per-bar and empty-bucket accessibility description templates (plain + plural forms as needed). (Header text reuses the existing `usage_history_by_day`/`usage_history_by_week` strings rather than duplicating identical wording; added translations for all 5 locales - de, es, fr, nl, pt.)
- [x] 4.2 Build a `TimeDeviationChart` composable in `ui/medicines/history/UsageChart.kt` (or a new sibling file in the same package), reusing the existing bar-layout pattern (`Row`/`Column`/`weight`, no `Canvas`), all bars in `secondaryContainer`/`onSecondaryContainer`, empty buckets rendered as an empty column that keeps its place.
- [x] 4.3 Build a `TimeDeviationChartCard` composable (header, chart, first/last date footer, overall-average figure and caption), shown only when `uiState.timeDeviation?.averageMinutes != null`.
- [x] 4.4 Add `TimeDeviationChartCard` to `MedicineHistoryScreen` below the existing `ChartCard`, inside the same non-empty branch, with test tags following the `MedicineHistoryTestTags` / `UsageChartTestTags` naming pattern.
- [x] 4.5 Add `@PreviewLightDark` previews: a period with a clear timing spread, a period with one or more empty (no-taken) buckets, and confirm no card is composed for a period with nothing taken (already covered by the existing empty-state previews needing no `TimeDeviationChartCard`).

## 5. Accessibility and verification

- [x] 5.1 Extend `MedicineHistoryScreenTest` (written and compiles; **not executed** - no `adb`/emulator available in this environment, see verification notes) (or add a sibling instrumented/semantics test) to assert the new chart's bar count, its accessibility descriptions for a populated and an empty bucket, and that the card is absent when nothing was taken.
- [x] 5.2 Run `pillsner-ui-review` against every changed/added file under `ui/medicines/history`.
- [x] 5.3 Run the unit test task and the lint task from the `src` Gradle project root and fix any failures before considering this change done. (`:app:testDebugUnitTest` and `:app:lintDebug` both pass; lint's 61 warnings are pre-existing baseline, none against the files this change touches.)
