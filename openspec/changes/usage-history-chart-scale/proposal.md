## Why

**GitHub Issue:** #35 (https://github.com/hexmasternl/pillsner/issues/35)
**Pull Request:** #36 (https://github.com/hexmasternl/pillsner/pull/36)

Both bar charts on the Usage history screen - the outcome chart and the timing accuracy chart added by `medicine-history-time-deviation` - show bars scaled relative to their own busiest bucket, but nothing on screen says what that busiest bar is actually worth. A sighted user can compare bars to each other but cannot read an actual number off either chart without reasoning it out from the figures elsewhere on the screen. A value axis fixes that with no new interaction, which keeps both charts read-only, consistent with the rest of this screen.

## What Changes

- Add a two-point value axis - the busiest bucket's value at the top, zero at the bottom - to the left of both bar charts on the Usage history screen: the existing outcome chart and the timing accuracy chart.
- Each axis label states its unit (doses for the outcome chart, minutes for the timing accuracy chart) so it reads correctly on its own, including to a screen reader that happens to land on it.
- No new interaction is added. Tapping a bar or a bucket to reveal a tooltip was considered and rejected: this screen is specified as read-only with no affordance beyond the period selector, and a tap-to-reveal tooltip would need dismiss handling and careful positioning at 200% font scale for no real gain, since the exact figure for any bucket is already available to a screen reader via the bar's existing spoken description.
- Explicitly in scope: the existing outcome chart, even though it predates this change and was not otherwise part of it - the user asked for the same fix to be applied there too, since the missing-scale problem is identical on both charts.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `medicine-usage-history`: adds a value-axis requirement for both bar charts on the Usage history screen. Written as `ADDED Requirements` rather than `MODIFIED`: the timing accuracy chart's own requirements live in the not-yet-archived `medicine-history-time-deviation` change and are not yet present in `openspec/specs/medicine-usage-history/spec.md` to modify against. Once both changes are archived the two requirement sets describe one coherent screen.

## Impact

- **UI**: a small shared axis composable used by both `UsageChart` and `TimeDeviationChart` (`ui/medicines/history/UsageChart.kt`), and the string resources for the axis labels, in all supported locales.
- **Domain**: none - the axis reads values the charts already compute (`busiest`), nothing new is calculated.
- **Persistence**: none.
