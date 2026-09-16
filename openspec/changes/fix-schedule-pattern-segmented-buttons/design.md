## Context

The schedule editor's pattern selector is a `SingleChoiceSegmentedButtonRow` with three equal-width `SegmentedButton`s (`ScheduleEditorScreen.kt`, ~line 180), labelled via `SchedulePattern.labelRes()` (~line 431) from `schedule_pattern_every_n_days`, `schedule_pattern_weekdays`, `schedule_pattern_every_n_hours`.

`app/build.gradle.kts` restricts packaged resources to `resourceConfigurations += listOf("en", "nl")`, so English and Dutch are the only two languages actually shipped today, even though `values-de`, `values-es`, `values-fr` and `values-pt` all exist in source (stripped at build time, presumably staged for a future language rollout). Comparing the packaged translations confirms the outer two labels are the longest, while the middle ("Weekdays"/"Vaste dagen") is always short:

| Locale | Every N days | Weekdays | Every N hours |
| --- | --- | --- | --- |
| en (shipped) | Every N days | Weekdays | Every N hours |
| nl (shipped) | Om de zoveel dagen | Vaste dagen | Om de zoveel uur |
| de (source only, not packaged) | Alle X Tage | Wochentage | Alle X Stunden |
| es (source only, not packaged) | Cada N días | Días fijos | Cada N horas |
| fr (source only, not packaged) | Tous les N jours | Jours fixes | Toutes les N heures |
| pt (source only, not packaged) | A cada N dias | Dias fixos | A cada N horas |

Compose's default `SegmentedButton` sizes each segment to equal width and lets its `Text` wrap independently. When only the outer segments wrap to two lines, each segment's content is centered within its own intrinsic height, so the row ends up with segments of visibly different heights and mis-aligned borders/centering — this is what issue #10 shows, reproducible today in Dutch. Shortening the strings is not a reliable fix: it would have to be re-verified for every current and future locale and at every system font scale, and the design system's accessibility rules disallow truncating or ellipsizing labels to force a fit. A fix tied to today's two shipped locales would also silently break again the moment `resourceConfigurations` grows to include any of the four already-translated-but-unpackaged languages, all of which have the same long-outer/short-middle shape.

## Goals / Non-Goals

**Goals:**
- All three segments in the pattern selector always render at one shared height, so the row stays visually aligned regardless of which (if any) label wraps.
- No label is ever truncated or ellipsized; if a label needs two lines, it gets two lines.
- The fix holds for both currently-shipped locales (en, nl) and, by construction, for the four source-only translations (de, es, fr, pt) whenever they are added to `resourceConfigurations`, and from the smallest to the largest system font scale.
- The existing single-choice, three-pattern behavior, string resources and test tags are unchanged.

**Non-Goals:**
- Reworking the copy/wording of the pattern labels.
- Changing which control type is used elsewhere in the app for similar choices.
- Any change to schedule domain logic, persistence, or the patterns offered.

## Decisions

**Decision: Force a uniform row height via `IntrinsicSize.Min`, and let every segment's label wrap onto as many lines as it needs, centered, with no line cap.**

- Wrap the `SingleChoiceSegmentedButtonRow` content in `Modifier.height(IntrinsicSize.Min)` and give each `SegmentedButton` `Modifier.fillMaxHeight()`, so every segment is measured against the tallest segment's content and stretches to match it — whether that tallest content is one line or several.
- Set each label `Text` to `textAlign = TextAlign.Center`, with no `maxLines` cap and no `overflow = TextOverflow.Ellipsis` (must not truncate).
- Result: whichever segment(s) need the most lines to fit their translated label drive the shared row height; segments with shorter labels are vertically centered within that same height. Borders and shape stay aligned because all segments now share one height, matching Material 3's expectation that segmented buttons in one row are the same height.

An earlier version of this decision capped labels at `maxLines = 2`, reasoning that two lines was enough headroom. Manually verifying at the largest system font scale (design system requirement, see Risks) disproved that: at 2x font scale in Dutch, "Om de zoveel uur" needs three lines, and `maxLines = 2` silently clipped the third line — the word "uur" disappeared with no ellipsis, which is truncation by another name. Removing the cap entirely closes that gap for any label, locale or font scale, at the cost of a potentially taller row; see Risks.

Alternatives considered:
- *Shrink font size to force one line (autosize text):* rejected — would make the pattern selector the only control in the app with a non-token font size, and per-locale/per-scale autosizing is harder to verify and screenshot-test than a wrapping layout.
- *Replace the segmented row with a different control (e.g., dropdown, vertically stacked options, or wrapping filter chips):* rejected as disproportionate for a layout bug — it would change an established interaction pattern users already know, and issue #10 only asks for the alignment to be fixed, not the control replaced. Worth reconsidering later if a future pattern is added and three segments become four or more.
- *Shorten the English/translated strings to guarantee one line:* rejected as unreliable long-term (new locales, font scale, dynamic text size can reintroduce wrapping) and not owned by this change (copy changes would need translator review).
- *Cap at `maxLines = 2`:* rejected after manual verification showed it truncates a label at large font scale (see above).

## Risks / Trade-offs

- [Multi-line labels make the row taller than today's single-line row in locales/font-scales that need to wrap] → Acceptable: the row already scrolls within the editor's `verticalScroll` column, and the design system's accessibility rule already requires everything to remain reachable by scrolling at the largest font scale.
- [`IntrinsicSize.Min` adds an extra measurement pass] → Negligible: the row has exactly three simple text-only children, no measurable performance impact.
- [A future locale added to `resourceConfigurations` could still wrap a segment that is currently one line] → Mitigated by design: the fix is locale-agnostic (any segment may wrap to any number of lines and the row still stays aligned), not a fix tuned to today's two shipped translations or a fixed line-count assumption.

