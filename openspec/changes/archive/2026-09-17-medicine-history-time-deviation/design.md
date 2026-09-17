## Context

The Usage history screen (`openspec/specs/medicine-usage-history/spec.md`) already computes, per period and per bucket, how many doses were taken, skipped, missed or left unanswered. It does this with `SummariseUsageHistory` (`domain/history/SummariseUsageHistory.kt`), which turns the medicine's stored `Dose`s into a `UsageHistory`/`UsageBucket` pair, and renders it with `UsageChart` inside `MedicineHistoryScreen`. Every `Dose` that was recorded taken already carries both `scheduledAt` (an `Instant`) and `intake.recordedAt` (the moment the user confirmed it) - `domain/model/Dose.kt`. Nothing new needs to be stored; this change only adds a second view over data that already exists.

The existing chart buckets "1 week" per day and both "1 month" and "3 months" per week (`UsagePeriod.bucketsByWeek`). The new chart follows a different rule the proposal asks for explicitly: "1 week" and "1 month" per day, "3 months" per week. The two charts will therefore sit on the same screen with different bucket widths for "1 month" - a deliberate choice for this metric (daily timing detail stays readable over a month; it would not for four extra weekly-outcome bars), not an inconsistency to reconcile.

## Goals / Non-Goals

**Goals:**
- Show, per bucket, the average number of minutes between a taken dose's scheduled moment and the moment it was recorded taken, as a bar chart on the existing Usage history screen.
- Show one overall average-deviation figure for the whole period, captioned so the user knows lower is better.
- Reuse the existing screen, card and chart patterns (layout, semantics-per-bar, locale-formatted dates, previews) rather than inventing new ones.
- Keep the domain calculation pure and unit-testable, like `SummariseUsageHistory`.

**Non-Goals:**
- No change to what is recorded for a dose or an intake - this is a read-only derived view.
- No change to the existing outcome chart's bucketing, colours or behaviour.
- No sign (early vs. late) on the deviation - the proposal is explicit that the figure is always a positive amount of minutes.
- No new screen, destination or navigation entry point.

## Decisions

**D1: A new domain model and summariser, not an extra field on `UsageHistory`.**
`UsageHistory`/`UsageBucket` describe dose *outcomes*; this feature describes *timing*, computed only from taken doses and with its own bucketing rule. Bolting it onto `UsageHistory` would force every bucket to carry two unrelated shapes and two unrelated bucket-alignment rules under one `isWeek` flag. Instead:
- `domain/model/TimeDeviation.kt` adds `TimeDeviationBucket(start: LocalDate, isWeek: Boolean, averageMinutes: Int?, takenCount: Int)` and `TimeDeviationHistory(period, firstDay, lastDay, averageMinutes: Int?, buckets: List<TimeDeviationBucket>)`. `averageMinutes` is null exactly when `takenCount` is zero, mirroring how `UsageHistory.adherencePercent` is null rather than zero when nothing is scheduled.
- `domain/history/SummariseTimeDeviation.kt` is a new pure class, sibling to `SummariseUsageHistory`, same `(clock, firstDayOfWeek)` constructor shape, same `operator fun invoke(period, doses): TimeDeviationHistory` style (no `earliestRecordedAt` parameter - the records-start note belongs to the outcome view, not this one).

**D2: Per-dose deviation is `abs(recordedAt - scheduledAt)`, rounded to the nearest minute.**
Truncating (as `Duration.toMinutes()` does) would silently make every deviation look better than it is by up to 59 seconds; rounding to nearest is a fairer average and matches the proposal's own examples (13 minutes late, 7 minutes early - both exact-minute cases where rounding and truncation agree, but rounding is correct in general). A bucket's average is the mean of its taken doses' per-dose deviations, itself rounded to the nearest whole minute for display. Skipped and missed doses have no `recordedAt` and are excluded entirely - they carry no timing information, not a zero.

**D3: The deviation chart's bucket boundaries are computed by a bucketing rule the two summarisers share.**
`SummariseUsageHistory` already contains the exact logic this needs - one bucket per day, or weekly buckets aligned to the locale's first day of the week with a short first bucket when the window opens mid-week - just keyed off a different per-period flag. Rather than copy that logic (and its day-boundary, leap-day and DST correctness) into a second file, it is extracted verbatim into an internal `domain/history/UsageBucketing.kt` with a `bucketStarts(firstDay, today, byWeek, firstDayOfWeek): List<LocalDate>` function and the matching `bucketIndex`. `SummariseUsageHistory` is changed only to call the extracted function instead of its private one; its existing tests must keep passing unchanged, since this is a pure move, not a behaviour change. `SummariseTimeDeviation` calls the same function with its own rule: `byWeek = period == UsagePeriod.THREE_MONTHS`.
- *Alternative considered*: duplicate the ~20 lines into the new file. Rejected - two copies of mid-week-alignment arithmetic is exactly the kind of thing that drifts and breaks silently at a leap day or DST boundary nobody re-tests twice.

**D4: The deviation chart is its own card, placed after the existing outcome chart card.**
`ChartCard` already owns the "how is this bucketed" header and the first/last date labels for the outcome chart; a second, differently-bucketed chart under the same header would misdescribe itself. A new `TimeDeviationChartCard` composable in the same file/package as `UsageChart` mirrors its structure (header, chart, footer dates) but states its own bucketing and carries the "less is better" caption. It is shown only when `timeDeviation.averageMinutes != null` (i.e. at least one dose was taken in the period); when null, the card is omitted and the rest of the screen (figures, breakdown, existing chart or empty state) is unaffected, per the proposal.

**D5: Bars use a single, non-outcome colour - `secondaryContainer`/`onSecondaryContainer`.**
The outcome chart's colours (`intakeStatusColors`) mean something specific (green = taken, red = missed, etc.); reusing green here would visually claim "13 minutes is a good result" which is not something this feature judges. `secondaryContainer` (Pillsner blue) is already used for a neutral, non-judgemental state (`Due`) and introduces no new hue, per the design system's colour rule. Every bar is the same colour; only height and the spoken minute count carry meaning.

**D6: Bar height is relative to the busiest bucket, exactly like the outcome chart.**
Reuses the pattern already proven in `UsageChart` (`headroom`/`weight` layout, no `Canvas`) so bars are legible at every font scale without a shared numeric axis. A bucket with `averageMinutes == null` renders as an empty column (its place in the row is kept, nothing is drawn) exactly as a bucket with `scheduled == 0` does today, so "no data" and "zero minutes off" are never visually confused.

**D7: Accessibility descriptions.**
Each bar is one semantics node describing its bucket's date (or week) and either "N minutes off schedule on average, over K doses" or, when empty, "no doses recorded taken" - worded to avoid implying the dose was missed (it may simply be outside this metric, e.g. all skipped). The overall figure and its "less is better" caption are plain text, read as any other figure on the screen.

## Risks / Trade-offs

- **[Risk]** Two different bucket widths for "1 month" (outcome chart weekly, deviation chart daily) on one screen could read as inconsistent. → Mitigation: each chart carries its own "per day" / "per week" header, exactly as the outcome chart already does for its own bucketing; no shared axis implies they must match.
- **[Risk]** Averaging minutes can be dominated by a single very late dose in a sparse bucket (e.g. one dose two hours late). → Mitigation: out of scope for this change - the proposal asks for a simple average, and the per-bar accessibility description states the dose count backing each average so a thin sample is visible, not hidden.
- **[Risk]** Extracting `bucketStarts`/`bucketIndex` out of `SummariseUsageHistory` touches code the archived `app-medicine-usage-history` change already shipped and tested. → Mitigation: the extraction is behaviour-preserving (same inputs, same outputs); the existing `SummariseUsageHistoryTest` suite is run unchanged as the regression check, and the extracted function gets its own focused tests alongside the new summariser's.

## Migration Plan

No data migration - purely additive UI and derived-data logic. No feature flag: the chart appears automatically wherever a taken dose exists in the selected period.

## Open Questions

None - the proposal and the existing usage-history spec resolve every design choice above.
