## 1. Domain: shared bucketing extraction

- [x] 1.1 Extract the bucket-boundary logic from `domain/history/SummariseUsageHistory.kt` into a new internal `domain/history/UsageBucketing.kt`, exposing `bucketStarts(firstDay, today, byWeek, firstDayOfWeek): List<LocalDate>` and the matching bucket-index lookup, preserving behaviour exactly.
- [x] 1.2 Update `SummariseUsageHistory` to call the extracted function instead of its private one.
- [x] 1.3 Run the existing `SummariseUsageHistoryTest` suite unchanged and confirm it still passes as the regression check for the extraction.
- [x] 1.4 Add focused unit tests for `UsageBucketing` covering daily buckets, weekly buckets aligned to the locale's first day of week, a short first bucket when the window opens mid-week, a leap day, and a daylight-saving transition.

## 2. Domain: timing deviation model and summariser

- [x] 2.1 Add `TimeDeviationBucket(start: LocalDate, isWeek: Boolean, averageMinutes: Int?, takenCount: Int)` and `TimeDeviationHistory(period, firstDay, lastDay, averageMinutes: Int?, buckets: List<TimeDeviationBucket>)` to a new `domain/model/TimeDeviation.kt`.
- [x] 2.2 Add `domain/history/SummariseTimeDeviation.kt`: a pure class with `(clock, firstDayOfWeek)` constructor and `operator fun invoke(period, doses): TimeDeviationHistory`, bucketing per day for "1 week"/"1 month" and per week for "3 months" via `UsageBucketing`.
- [x] 2.3 Compute each taken dose's deviation as `abs(recordedAt - scheduledAt)` rounded to the nearest minute; exclude skipped and missed doses entirely (no `recordedAt`).
- [x] 2.4 Compute each bucket's `averageMinutes` as the mean of its taken doses' deviations, rounded to the nearest minute, or `null` when `takenCount == 0`.
- [x] 2.5 Compute the overall period `averageMinutes` the same way across all taken doses in the window, or `null` when none were taken.
- [x] 2.6 Unit test `SummariseTimeDeviation`: early vs. late both contribute the same positive minutes, skipped/missed doses are excluded, an empty bucket yields `null` not zero, an entirely-untaken period yields `null` overall, daily vs. weekly bucketing per period, and rounding at the half-minute boundary.

## 3. UI: shared value axis

- [x] 3.1 Add `ChartValueAxis(topLabel: String, bottomLabel: String, modifier)` to `UsageChart.kt`: an unweighted column, `fillMaxHeight()`, `Arrangement.SpaceBetween` between the two labels, placed as the first (leading) child of the bars' `Row`.
- [x] 3.2 Add the `usage_history_axis_doses` plural string resource (and translations for every supported locale) for the outcome chart's axis; reuse the timing accuracy chart's own average-minutes plural resource for its axis.
- [x] 3.3 Wire `ChartValueAxis` into the existing outcome chart (`UsageChart`) using the busiest bucket's scheduled count and the new doses string, and into the new timing accuracy chart using the busiest bucket's average minutes.
- [x] 3.4 Confirm neither axis label is merged into a bar's semantics node, so each reads its own unit correctly in isolation for a screen reader.

## 4. UI: timing accuracy chart

- [x] 4.1 Add a `TimeDeviationChartCard` composable in `ui/medicines/history/UsageChart.kt` (or an adjacent file in the same package), mirroring `ChartCard`'s header/chart/footer-dates structure, stating its own per-day/per-week bucketing and carrying the "less is better" caption.
- [x] 4.2 Render one bar per bucket, height relative to the busiest bucket's `averageMinutes`, using a single neutral colour (`secondaryContainer`/`onSecondaryContainer`) distinct from the outcome chart's intake-status colours.
- [x] 4.3 Render a bucket with `averageMinutes == null` as an empty column that still holds its place in the row.
- [x] 4.4 Give each bar a single accessibility node describing its bucket's date/week and either the average-minutes-and-count or "no doses recorded taken" wording.
- [x] 4.5 Show the overall average-deviation figure and its "lower is better" caption alongside the chart.
- [x] 4.6 Omit the entire card (chart, overall figure, caption) when `timeDeviation.averageMinutes == null`.
- [x] 4.7 Add the required string resources (card header, "less is better" caption, per-bar accessibility templates, empty-bucket description) to `strings.xml` and every supported locale.

## 5. Wiring: view model and screen

- [x] 5.1 Add `TimeDeviationHistory` to `MedicineHistoryUiState` and compute it in `MedicineHistoryViewModel` alongside the existing `UsageHistory`, recomputing on period change exactly as the outcome data does.
- [x] 5.2 Place the `TimeDeviationChartCard` on `MedicineHistoryScreen` after the existing outcome chart card, only when the screen is not showing the empty state.
- [x] 5.3 Add preview data for the timing accuracy chart to `UsageHistoryPreviewData.kt` covering a populated period, a period with an empty bucket, and a period with nothing taken.
- [x] 5.4 Add Compose previews for `TimeDeviationChartCard` and for both charts' `ChartValueAxis` at default and largest font scale.

## 6. Verification

- [x] 6.1 Run `pillsner-ui-review` against every changed/added composable and resolve any violations.
- [x] 6.2 Add or update semantics-based UI tests in `MedicineHistoryScreenTest.kt` covering: the timing accuracy chart appears/disappears correctly, its bucketing per period, its accessibility descriptions, and both charts' value axes.
- [x] 6.3 Run the domain unit tests, the Compose UI tests and lint from the `src` Gradle project root; fix any failures. Domain unit tests (42 tests across `SummariseUsageHistoryTest`, `SummariseTimeDeviationTest`, `UsageBucketingTest`), the full `testDebugUnitTest` suite and `lintDebug` all pass. The instrumented `MedicineHistoryScreenTest` compiles cleanly (`compileDebugAndroidTestKotlin`) but could not be executed here: no emulator/device was attached (`adb devices` returned none). Run `./gradlew :app:connectedDebugAndroidTest` on a device before merging.
- [ ] 6.4 Manually verify at the largest system font scale that the new card, chart, axis and overall figure scroll fully into view with no clipped or truncated text.
