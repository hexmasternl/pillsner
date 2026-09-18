## 1. Strings

- [x] 1.1 Add the `usage_history_axis_doses` plural resource ("%1$d dose" / "%1$d doses") to `values/strings.xml` and translate it into all five supported locales (de, es, fr, nl, pt).

## 2. Shared axis composable

- [x] 2.1 Add `ChartValueAxis(topLabel: String, bottomLabel: String, modifier)` to `ui/medicines/history/UsageChart.kt`: a `Column` sized to its own content width, `fillMaxHeight()` to match the bar row, `Arrangement.SpaceBetween` between the two labels, `bodySmall`/`onSurfaceVariant` styling matching the existing first/last date labels.

## 3. Wire the axis into both charts

- [x] 3.1 In `UsageChart`, add `ChartValueAxis` as the first, unweighted item of the bars' `Row`, with `topLabel` from `usage_history_axis_doses` using `busiest` and `bottomLabel` from the same plural with 0.
- [x] 3.2 In `TimeDeviationChart`, add `ChartValueAxis` the same way, with both labels from the existing `usage_history_timing_average` plural (using `busiest` and 0), guarded the same way the chart already omits itself when there is nothing to show.
- [x] 3.3 Add test tags for the axis (e.g. `AXIS_TOP` / `AXIS_BOTTOM` per chart's test-tags object) so semantics tests can reach the two labels.

## 4. Tests and previews

- [x] 4.1 Extend the `MedicineHistoryScreenTest` (written and compiles; **not executed** - no `adb`/emulator available in this environment) timing-accuracy and outcome-chart tests to assert the axis top/bottom labels are displayed with the expected localized, pluralized text for a known fixture.
- [x] 4.2 Update the existing `@PreviewLightDark` previews for `UsageChart` and `TimeDeviationChart` if the added axis changes their visual balance enough to warrant a look (no new preview variants required - the axis has no state of its own). Both previews now render the axis automatically since it is part of each chart composable; no preview code changes were needed.

## 5. Verification

- [x] 5.1 Run `pillsner-ui-review` against every changed file under `ui/medicines/history`.
- [x] 5.2 Run the unit test task and the lint task from the `src` Gradle project root and fix any failures before considering this change done. (`:app:testDebugUnitTest` and `:app:lintDebug` both pass with zero errors; lint's warning count is unrelated pre-existing baseline noise - nothing in the report names the new file, composable or strings.)
