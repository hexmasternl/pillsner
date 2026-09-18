## Context

`UsageChart` (outcome bars) and `TimeDeviationChart` (timing accuracy bars) both live in `ui/medicines/history/UsageChart.kt` and share the same shape: a `Row` of bar columns inside a fixed-height container (`Sizes.usageChartHeight`, 96 dp), each bar's height a fraction of a `busiest` value already computed in the composable (`history.buckets.maxOfOrNull { it.scheduled }` and `history.buckets.mapNotNull { it.averageMinutes }.maxOrNull()` respectively). Neither chart currently shows what that busiest value is - only its relative proportion. The exact figure for any single bucket already reaches a screen reader through each bar's merged `contentDescription`; what is missing is a way for a sighted user to read even an approximate number without invoking assistive tech.

## Goals / Non-Goals

**Goals:**
- Give both charts a visible scale: what the tallest bar is worth, and that zero is the baseline.
- Keep both charts read-only, matching the Usage history screen's existing contract (no action beyond the period selector).
- Share one axis implementation between the two charts rather than writing it twice.

**Non-Goals:**
- No tap-to-reveal tooltip. Rejected in the proposal: it's a new interaction on a screen specified as read-only, and needs dismiss handling, positioning that survives 200% font scale and one-handed reach, for information a screen reader can already give exactly.
- No multi-tick axis (0/25/50/75/100 %) with gridlines across the chart. A single top/bottom pair is enough to anchor the eye and stays legible at every font scale; a denser axis risks clutter behind up to fourteen weekly bars and adds little a screen reader user doesn't already have.
- No change to bar proportions, colours or bucketing - this only adds a label column beside the existing bars.

## Decisions

**D1: A shared `ChartValueAxis` composable, not two separate implementations.**
Both charts need the identical shape - a value at the top, "0" at the bottom, vertically matching the bar row's height - differing only in which localized, pluralized string each passes in. `ChartValueAxis(topLabel: String, bottomLabel: String, modifier)` goes in `UsageChart.kt` alongside the two chart composables that use it, mirroring the `UsageBucketing` precedent of extracting one rule two features need identically rather than duplicating it.
- *Alternative considered*: write the axis inline in each chart. Rejected - it's the same eight lines of layout twice, and any future fix (e.g. an accessibility tweak) would need to land in both places.

**D2: The axis is the first item in the bars' `Row`, unweighted, sized to its own content.**
The bar columns already use `Modifier.weight(1f)` to divide the remaining width evenly; adding the axis as a plain (non-weighted) sibling before them lets `Row` measure it at its natural content width first and give everything else to the bars, with no manual width arithmetic. It also gets Compose's default RTL mirroring for free, since it sits at the layout's leading edge rather than a hand-picked side.
- *Alternative considered*: a fixed `Dp` width for the axis column. Rejected - a fixed width either clips a longer localized string (German "22 Minuten" is longer than English "22 minutes") or wastes space for a short one; content-sized avoids both without a locale-specific constant.

**D3: Both axis labels carry their unit, not a bare "0".**
The axis Text nodes are ordinary text, not merged into a bar's semantics node, so a screen reader landing on the bottom label in isolation would read whatever it says. "0 doses" / "0 minutes" is self-explanatory on its own; a bare "0" is not. The top label reuses the same plural resource with the busiest value; the timing accuracy chart reuses the plural string `usage_history_timing_average` already introduced by `medicine-history-time-deviation`, and the outcome chart gets one new plural, `usage_history_axis_doses`.

**D4: The axis column is not height-clamped to the fixed 96 dp chart height; only the bar row is.**
The bar row's fixed height is what makes relative bar proportions meaningful and must stay as-is. The axis column sits beside it at `fillMaxHeight()` with `Arrangement.SpaceBetween` between its two labels, but is not given a narrow fixed width or wrapped, so its (short) text has room to grow at large font scales without clipping or forcing a second line inside a height that isn't built to hold one.

**D5: The spec delta is written as `ADDED Requirements`, not `MODIFIED`.**
`medicine-history-time-deviation` is implemented on this same branch but not yet archived, so its "Timing accuracy chart" requirement is not yet present in `openspec/specs/medicine-usage-history/spec.md` for this change to modify against - only in that change's own pending delta. Rather than have this change reach into another change's in-flight delta, its own two requirements are added standalone, one naming the outcome chart, one naming the timing accuracy chart. Once both changes archive (in either order), the combined `specs/medicine-usage-history/spec.md` describes one coherent pair of charts, each with its own axis requirement.

## Risks / Trade-offs

- **[Risk]** A two-label axis only anchors the eye at the extremes; a middle bar's exact value still has to be estimated visually. → Mitigation: this is the trade-off the proposal accepted over a tooltip; the exact number for any bucket remains available via the bar's spoken description for anyone who needs precision rather than an estimate.
- **[Risk]** Adding an axis column narrows the width left for bars, most noticeably in the "3 months" timing chart with up to fourteen weekly bars. → Mitigation: bars carry no text of their own, so narrowing is a visual-density change only, not a truncation risk; this matches how the chart already handles narrow bars today.
- **[Risk]** Archiving `usage-history-chart-scale` before `medicine-history-time-deviation` would put an `ADDED` "Timing accuracy chart value axis" requirement into `specs/medicine-usage-history/spec.md` ahead of the "Timing accuracy chart" requirement it depends on. → Mitigation: both changes are implemented on the same branch and are expected to ship in the same release; whoever archives them keeps that order in mind, and the requirement text names the chart it augments plainly enough to read sensibly either way.

## Migration Plan

No data migration - purely additive UI. No feature flag: the axis appears automatically alongside both charts.

## Open Questions

None.
