## Context

The Usage history screen (`openspec/specs/medicine-usage-history/spec.md`) already computes, per period and per bucket, how many doses were taken, skipped, missed or left unanswered. It does this with `SummariseUsageHistory` (`domain/history/SummariseUsageHistory.kt`), which turns the medicine's stored `Dose`s into a `UsageHistory`/`UsageBucket` pair, and renders it with `UsageChart` inside `MedicineHistoryScreen`. Every `Dose` that was recorded taken already carries both `scheduledAt` (an `Instant`) and `intake.recordedAt` (the moment the user confirmed it) - `domain/model/Dose.kt`. Nothing new needs to be stored; this change only adds a second view over data that already exists, plus a visible scale for both views.

The existing chart buckets "1 week" per day and both "1 month" and "3 months" per week (`UsagePeriod.bucketsByWeek`). The new chart follows a different rule this proposal asks for explicitly: "1 week" and "1 month" per day, "3 months" per week. The two charts will therefore sit on the same screen with different bucket widths for "1 month" - a deliberate choice for this metric (daily timing detail stays readable over a month; it would not for four extra weekly-outcome bars), not an inconsistency to reconcile.

Neither chart currently shows what its tallest bar is worth, only its relative proportion. The exact figure for any single bucket already reaches a screen reader through each bar's merged `contentDescription`; what is missing is a way for a sighted user to read even an approximate number without invoking assistive tech.

This consolidates two previously separate, already-implemented changes (`medicine-history-time-deviation` for issue #29, `usage-history-chart-scale` for issue #35) into one, since both were lost from `main` in an unrelated revert (see proposal). The design decisions below carry those two changes' reasoning forward largely unchanged, now written as one coherent design against a spec that has neither chart yet.

## Goals / Non-Goals

**Goals:**
- Show, per bucket, the average number of minutes between a taken dose's scheduled moment and the moment it was recorded taken, as a bar chart on the existing Usage history screen.
- Show one overall average-deviation figure for the whole period, captioned so the user knows lower is better.
- Give both bar charts on the screen - the existing outcome chart and the new timing accuracy chart - a visible value axis: what the tallest bar is worth, and that zero is the baseline.
- Reuse the existing screen, card and chart patterns (layout, semantics-per-bar, locale-formatted dates, previews) rather than inventing new ones.
- Keep the domain calculation pure and unit-testable, like `SummariseUsageHistory`.

**Non-Goals:**
- No change to what is recorded for a dose or an intake - this is a read-only derived view.
- No change to the existing outcome chart's bucketing, colours or behaviour beyond adding its axis.
- No sign (early vs. late) on the deviation - the figure is always a positive amount of minutes.
- No new screen, destination or navigation entry point.
- No tap-to-reveal tooltip on either chart. Rejected: it's a new interaction on a screen specified as read-only, and would need dismiss handling and positioning that survives 200% font scale and one-handed reach, for information a screen reader can already give exactly.
- No multi-tick axis (0/25/50/75/100%) with gridlines. A single top/bottom pair anchors the eye and stays legible at every font scale; a denser axis risks clutter behind up to fourteen weekly bars and adds little a screen reader user doesn't already have.

## Decisions

**D1: A new domain model and summariser, not an extra field on `UsageHistory`.**
`UsageHistory`/`UsageBucket` describe dose *outcomes*; this feature describes *timing*, computed only from taken doses and with its own bucketing rule. Bolting it onto `UsageHistory` would force every bucket to carry two unrelated shapes and two unrelated bucket-alignment rules under one `isWeek` flag. Instead:
- `domain/model/TimeDeviation.kt` adds `TimeDeviationBucket(start: LocalDate, isWeek: Boolean, averageMinutes: Int?, takenCount: Int)` and `TimeDeviationHistory(period, firstDay, lastDay, averageMinutes: Int?, buckets: List<TimeDeviationBucket>)`. `averageMinutes` is null exactly when `takenCount` is zero, mirroring how `UsageHistory.adherencePercent` is null rather than zero when nothing is scheduled.
- `domain/history/SummariseTimeDeviation.kt` is a new pure class, sibling to `SummariseUsageHistory`, same `(clock, firstDayOfWeek)` constructor shape, same `operator fun invoke(period, doses): TimeDeviationHistory` style (no `earliestRecordedAt` parameter - the records-start note belongs to the outcome view, not this one).

**D2: Per-dose deviation is `abs(recordedAt - scheduledAt)`, rounded to the nearest minute.**
Truncating (as `Duration.toMinutes()` does) would silently make every deviation look better than it is by up to 59 seconds; rounding to nearest is a fairer average. A bucket's average is the mean of its taken doses' per-dose deviations, itself rounded to the nearest whole minute for display. Skipped and missed doses have no `recordedAt` and are excluded entirely - they carry no timing information, not a zero.

**D3: The deviation chart's bucket boundaries are computed by a bucketing rule the two summarisers share.**
`SummariseUsageHistory` already contains the exact logic this needs - one bucket per day, or weekly buckets aligned to the locale's first day of the week with a short first bucket when the window opens mid-week - just keyed off a different per-period flag. Rather than copy that logic (and its day-boundary, leap-day and DST correctness) into a second file, it is extracted verbatim into an internal `domain/history/UsageBucketing.kt` with a `bucketStarts(firstDay, today, byWeek, firstDayOfWeek): List<LocalDate>` function and the matching `bucketIndex`. `SummariseUsageHistory` is changed only to call the extracted function instead of its private one; its existing tests must keep passing unchanged, since this is a pure move, not a behaviour change. `SummariseTimeDeviation` calls the same function with its own rule: `byWeek = period == UsagePeriod.THREE_MONTHS`.
- *Alternative considered*: duplicate the ~20 lines into the new file. Rejected - two copies of mid-week-alignment arithmetic is exactly the kind of thing that drifts and breaks silently at a leap day or DST boundary nobody re-tests twice.

**D4: The deviation chart is its own card, placed after the existing outcome chart card.**
`ChartCard` already owns the "how is this bucketed" header and the first/last date labels for the outcome chart; a second, differently-bucketed chart under the same header would misdescribe itself. A new `TimeDeviationChartCard` composable in the same file/package as `UsageChart` mirrors its structure (header, chart, footer dates) but states its own bucketing and carries the "less is better" caption. It is shown only when `timeDeviation.averageMinutes != null` (i.e. at least one dose was taken in the period); when null, the card is omitted and the rest of the screen (figures, breakdown, existing chart or empty state) is unaffected.

**D5: Bars use a single, non-outcome colour - `secondaryContainer`/`onSecondaryContainer`.**
The outcome chart's colours (`intakeStatusColors`) mean something specific (green = taken, red = missed, etc.); reusing green here would visually claim "13 minutes is a good result" which is not something this feature judges. `secondaryContainer` (Pillsner blue) is already used for a neutral, non-judgemental state (`Due`) and introduces no new hue, per the design system's colour rule. Every bar is the same colour; only height and the spoken minute count carry meaning.

**D6: Bar height is relative to the busiest bucket, exactly like the outcome chart.**
Reuses the pattern already proven in `UsageChart` (`headroom`/`weight` layout, no `Canvas`) so bars are legible at every font scale without a shared numeric axis doing the scaling work. A bucket with `averageMinutes == null` renders as an empty column (its place in the row is kept, nothing is drawn) exactly as a bucket with `scheduled == 0` does today, so "no data" and "zero minutes off" are never visually confused.

**D7: Accessibility descriptions.**
Each bar is one semantics node describing its bucket's date (or week) and either "N minutes off schedule on average, over K doses" or, when empty, "no doses recorded taken" - worded to avoid implying the dose was missed (it may simply be outside this metric, e.g. all skipped). The overall figure and its "less is better" caption are plain text, read as any other figure on the screen.

**D8: A shared `ChartValueAxis` composable, not two separate implementations.**
Both charts need the identical shape - a value at the top, "0" at the bottom, vertically matching the bar row's height - differing only in which localized, pluralized string each passes in. `ChartValueAxis(topLabel: String, bottomLabel: String, modifier)` goes in `UsageChart.kt` alongside the two chart composables that use it, following the same extract-one-shared-rule precedent as `UsageBucketing`.
- *Alternative considered*: write the axis inline in each chart. Rejected - it's the same eight lines of layout twice, and any future fix (e.g. an accessibility tweak) would need to land in both places.

**D9: The axis is the first item in the bars' `Row`, unweighted, sized to its own content.**
The bar columns already use `Modifier.weight(1f)` to divide the remaining width evenly; adding the axis as a plain (non-weighted) sibling before them lets `Row` measure it at its natural content width first and give everything else to the bars, with no manual width arithmetic. It also gets Compose's default RTL mirroring for free, since it sits at the layout's leading edge rather than a hand-picked side.
- *Alternative considered*: a fixed `Dp` width for the axis column. Rejected - a fixed width either clips a longer localized string (German "22 Minuten" is longer than English "22 minutes") or wastes space for a short one; content-sized avoids both without a locale-specific constant.

**D10: Both axis labels carry their unit, not a bare "0".**
The axis `Text` nodes are ordinary text, not merged into a bar's semantics node, so a screen reader landing on the bottom label in isolation would read whatever it says. "0 doses" / "0 minutes" is self-explanatory on its own; a bare "0" is not. The top label reuses the same plural resource with the busiest value; the timing accuracy chart reuses the plural string introduced for its own average figure, and the outcome chart gets one new plural, `usage_history_axis_doses`.

**D11: The axis column is not height-clamped to the fixed chart height; only the bar row is.**
The bar row's fixed height (`Sizes.usageChartHeight`, 96 dp) is what makes relative bar proportions meaningful and must stay as-is. The axis column sits beside it at `fillMaxHeight()` with `Arrangement.SpaceBetween` between its two labels, but is not given a narrow fixed width or wrapped, so its (short) text has room to grow at large font scales without clipping or forcing a second line inside a height that isn't built to hold one.

## Risks / Trade-offs

- **[Risk]** Two different bucket widths for "1 month" (outcome chart weekly, deviation chart daily) on one screen could read as inconsistent. → Mitigation: each chart carries its own "per day" / "per week" header, exactly as the outcome chart already does for its own bucketing; no shared axis implies they must match.
- **[Risk]** Averaging minutes can be dominated by a single very late dose in a sparse bucket (e.g. one dose two hours late). → Mitigation: out of scope for this change - a simple average is asked for, and the per-bar accessibility description states the dose count backing each average so a thin sample is visible, not hidden.
- **[Risk]** Extracting `bucketStarts`/`bucketIndex` out of `SummariseUsageHistory` touches code the existing usage-history feature already shipped and tested. → Mitigation: the extraction is behaviour-preserving (same inputs, same outputs); the existing `SummariseUsageHistoryTest` suite is run unchanged as the regression check, and the extracted function gets its own focused tests alongside the new summariser's.
- **[Risk]** A two-label axis only anchors the eye at the extremes; a middle bar's exact value still has to be estimated visually. → Mitigation: this is an accepted trade-off over a tooltip; the exact number for any bucket remains available via the bar's spoken description for anyone who needs precision rather than an estimate.
- **[Risk]** Adding an axis column narrows the width left for bars, most noticeably in the "3 months" timing chart with up to fourteen weekly bars. → Mitigation: bars carry no text of their own, so narrowing is a visual-density change only, not a truncation risk; this matches how the chart already handles narrow bars today.

## Migration Plan

No data migration - purely additive UI and derived-data logic. No feature flag: both the timing accuracy chart and the value axes appear automatically wherever their data exists (a taken dose in the period, and any chart at all, respectively).

## Open Questions

None - the proposal and the existing usage-history spec resolve every design choice above.
