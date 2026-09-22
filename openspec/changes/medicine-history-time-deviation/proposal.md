## Why

**GitHub Issue:** #29 (https://github.com/hexmasternl/pillsner/issues/29)

The Usage history screen tells the user *whether* a dose was taken, but not *how close to on time* it was. Someone whose adherence is 100% by outcome could still be taking every dose forty minutes late, and has no way to see that drift. A bar chart of the average gap between scheduled and actual intake time gives that visibility without adding any new data to record - the moments already exist on every taken dose. Neither this new chart nor the existing outcome chart currently shows what its tallest bar is actually worth, only bars relative to each other, so a sighted user cannot read even an approximate figure off either chart without hunting for it elsewhere on the screen.

This reimplements work that was previously designed, built and merged (as the separate `medicine-history-time-deviation` and `usage-history-chart-scale` changes) but was lost from `main` when an unrelated production crash (an ML Kit/GMS permission leak from the label-scan feature) forced a revert of `main` back to the last known-good release. The feature itself was sound; it is being redone here as a single change under this issue so both pieces land together.

## What Changes

- Add a "timing accuracy" bar chart to the Usage history screen showing, per bucket, the average number of minutes between a dose's scheduled time and the time it was recorded taken. The deviation is always shown as a positive amount of minutes regardless of whether the dose was taken early or late; only taken doses contribute, since skipped and missed doses have no actual intake time.
- Show an overall average-deviation figure for the period alongside the chart, with a caption stating that a lower number is better.
- Bucket the deviation chart per day for the "1 week" and "1 month" periods, and per week for the "3 months" period. This is a different bucketing rule from the existing outcome chart (which buckets "1 month" by week), chosen because a month of daily timing detail is still readable and more useful than a month of weekly averages.
- When a bucket holds no taken dose, it is shown as an empty column rather than a zero-minute bar, so "nothing to measure" is never confused with "perfectly on time".
- When no dose has been taken anywhere in the period, the timing accuracy chart is omitted entirely; the rest of the Usage history screen (figures, breakdown, existing chart or empty state) is unaffected.
- Add a two-point value axis - the busiest bucket's value at the top, zero at the bottom - to the left of both bar charts on the Usage history screen: the existing outcome chart and the new timing accuracy chart. Each axis label states its unit (doses for the outcome chart, minutes for the timing accuracy chart) so it reads correctly on its own, including to a screen reader that happens to land on it.
- No new interaction beyond the existing period selector. Tapping a bar or a bucket to reveal a tooltip is explicitly out of scope: this screen is read-only, and the exact figure for any bucket is already available to a screen reader via the bar's existing spoken description.

## Capabilities

### New Capabilities
(none - this extends the existing usage history capability rather than introducing a new one)

### Modified Capabilities
- `medicine-usage-history`: adds a timing-accuracy chart (a new chart, its own bucketing rule, its own accessibility descriptions and its own empty-data handling) alongside the existing outcome chart, and adds a value-axis requirement covering both bar charts on the screen.

## Impact

- **Domain**: a new pure calculation next to `SummariseUsageHistory` that turns a medicine's doses into per-bucket average deviation minutes; a new domain model alongside `UsageHistory`/`UsageBucket`. The mid-week-aligned, locale-aware bucket-boundary logic already in `SummariseUsageHistory` is extracted into a small shared helper so the two summarisers agree on bucket edges without duplicating that logic.
- **UI**: a new chart composable and card on `MedicineHistoryScreen`, wired into `MedicineHistoryViewModel`/`MedicineHistoryUiState`; a shared value-axis composable used by both the new and the existing chart. New string resources for the chart's header, the "less is better" caption, per-bar accessibility descriptions and the axis unit labels. No new screen, no new navigation, no new dependency.
- **Persistence**: none - the deviation is derived entirely from `Dose.scheduledAt` and `Intake.recordedAt`, which are already stored.
