## Why

**GitHub Issue:** #29 (https://github.com/hexmasternl/pillsner/issues/29)
**Pull Request:** #36 (https://github.com/hexmasternl/pillsner/pull/36)

The Usage history screen tells the user *whether* a dose was taken, but not *how close to on time* it was. Someone whose adherence is 100% by outcome could still be taking every dose forty minutes late, and has no way to see that drift. A bar chart of the average gap between scheduled and actual intake time gives that visibility without adding any new data to record - the moments already exist on every taken dose.

## What Changes

- Add a "timing accuracy" bar chart to the Usage history screen showing, per bucket, the average number of minutes between a dose's scheduled time and the time it was recorded taken. The deviation is always shown as a positive amount of minutes regardless of whether the dose was taken early or late; only taken doses contribute, since skipped and missed doses have no actual intake time.
- Show an overall average-deviation figure for the period alongside the chart, with a caption stating that a lower number is better.
- Bucket the deviation chart per day for the "1 week" and "1 month" periods, and per week for the "3 months" period. This is a different bucketing rule from the existing outcome chart (which buckets "1 month" by week), chosen because a month of daily timing detail is still readable and more useful than a month of weekly averages.
- When a bucket holds no taken dose, it is shown as an empty column rather than a zero-minute bar, so "nothing to measure" is never confused with "perfectly on time".
- When no dose has been taken anywhere in the period, the timing accuracy chart is omitted entirely; the rest of the Usage history screen (figures, breakdown, existing chart or empty state) is unaffected.

## Capabilities

### New Capabilities
(none - this extends the existing usage history capability rather than introducing a new one)

### Modified Capabilities
- `medicine-usage-history`: adds a timing-accuracy requirement set (a new chart, its bucketing rule, its own accessibility descriptions and its empty-data handling) alongside the existing outcome chart on the same screen.

## Impact

- **Domain**: a new pure calculation next to `SummariseUsageHistory` that turns a medicine's doses into per-bucket average deviation minutes; a new domain model alongside `UsageHistory`/`UsageBucket`. The mid-week-aligned, locale-aware bucket-boundary logic already in `SummariseUsageHistory` is extracted into a small shared helper so the two summarisers agree on bucket edges without duplicating that logic.
- **UI**: a new chart composable and card on `MedicineHistoryScreen`, wired into `MedicineHistoryViewModel`/`MedicineHistoryUiState`. New string resources for the chart's header, the "less is better" caption and per-bar accessibility descriptions. No new screen, no new navigation, no new dependency.
- **Persistence**: none - the deviation is derived entirely from `Dose.scheduledAt` and `Intake.recordedAt`, which are already stored.
