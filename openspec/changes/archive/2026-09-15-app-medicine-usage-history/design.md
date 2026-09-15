## Context

The `doses` table is already a complete record of the past. `RefreshPlannedDoses` only ever withdraws doses that are still merely planned, and only inside a window of today and tomorrow plus a day of slack on each side; a dose that has been answered or reminded about is never touched, and a dose older than that window is never deleted at all. `MarkMissedDoses` settles every unanswered dose that lapses. So for any medicine, the rows with a `scheduled_at` in the past are exactly what happened, with a `TAKEN`, `SKIPPED` or `MISSED` outcome, or briefly nothing while the dose waits to lapse.

Nothing in the app reads that. `DoseDao` has queries for pending doses, for the next moment, and for the reminder path; there is no query over a date range for one medicine. The `(medication_id, scheduled_at)` unique index that makes refreshing idempotent happens to be the exact index such a query wants.

The Medicine details screen is `MedicationFormScreen` in edit mode, inside `MedicationFormGraph` — a nested navigation graph holding the form and the schedule editor around one shared draft, so the draft survives rotation and process death for as long as the flow is on the back stack. Its `TopAppBar` today has a title and a back arrow and nothing else. The `medicine-details` spec is explicit that no delete, remove or archive action may exist anywhere on that screen, in its menus or in its dialogs — a constraint any new menu inherits.

Constraints from the repository: the domain layer has no Android dependencies; every colour, size and text style comes from a theme token in `docs/design-system.md`; user-facing text lives in string resources with a complete Dutch translation or lint fails; medication names and dosages are never logged at info level or above; nothing leaves the device.

## Goals / Non-Goals

**Goals:**

- Answer "have I been taking this?" for one medicine, over a week, a month or three months, in one glance and without any interpretation on the user's part.
- Separate the four things that can become of a dose — taken, skipped, missed, still unanswered — because a skipped dose is a choice and not a failure, and the screen must not imply otherwise.
- Be honest about how far back the records go, so a low count is never mistaken for poor adherence.
- Put the date arithmetic in a pure domain summariser so midnight, month ends, leap days, daylight saving and time zone moves are unit-tested rather than hoped for.
- Add nothing to the schema, nothing to the permission set, and nothing to the dependency list.

**Non-Goals:**

- Adherence across all medicines, a global statistics screen, trends, streaks, targets or nudges. This change is one medicine's record, on request.
- Editing history. Nothing on this screen writes; a dose answered in the past stays as it is.
- Export, sharing or printing. That is a separate proposal with a privacy argument of its own.
- Periods other than the three named, a custom date range, or a per-dose-time breakdown ("you miss your evening dose"). Worth having later; not needed to make the record visible.
- Reaching the history from anywhere but the details screen — no entry from the overview, Home or a notification.

## Decisions

### D1. Two read queries, no schema change

`DoseDao` gains a range query for one medicine, returned as a `Flow<List<DoseEntity>>`:

```sql
SELECT * FROM doses
WHERE medication_id = :medicationId AND scheduled_at >= :from AND scheduled_at < :to
ORDER BY scheduled_at ASC
```

and the medicine's earliest recorded moment, as a `suspend fun earliestScheduledAt(medicationId: Long): Instant?`:

```sql
SELECT MIN(scheduled_at) FROM doses WHERE medication_id = :medicationId
```

`DoseRepository` gains `observeHistoryFor(medicationId, from, to): Flow<List<Dose>>` and `earliestScheduledAt(medicationId): Instant?`; `RoomDoseRepository` and `InMemoryDoseRepository` implement both. No `DoseEntity` field changes, no `PillsnerDatabase` version bump, no migration.

The second query is what lets the screen say "records start 3 September" instead of silently showing three months of mostly nothing. It cannot be derived from the first: a medicine taken weekly legitimately has empty stretches, so an empty leading stretch is not evidence that records are missing.

Alternative considered: a single snapshot `suspend fun` rather than a `Flow`. Rejected — a dose answered from a notification while the screen is open should be reflected, and Room gives the invalidation for free.

### D2. Period and window

`UsagePeriod` is a domain enum with three members, `WEEK`, `MONTH` and `THREE_MONTHS`. A period resolves against a `Clock` to a window of local dates ending today:

| Period | First day | Days |
| --- | --- | --- |
| `WEEK` | `today.minusDays(6)` | 7 |
| `MONTH` | `today.minusMonths(1).plusDays(1)` | 28–31 |
| `THREE_MONTHS` | `today.minusMonths(3).plusDays(1)` | 89–92 |

The window as instants is `[startOfDay(firstDay, zone), now]`. The upper bound is **now**, not the end of today: a dose due at 20:00 has not happened at lunchtime and must not be counted as a dose the user failed to take. Using `LocalDate` arithmetic and `atStartOfDay(zone)` rather than subtracting a fixed number of hours is what makes month ends, leap days and daylight-saving transitions come out right for free.

Alternative considered: 7, 30 and 90 days. Rejected — "1 month" that ends on a different day of the month than it started is harder to reason about than `minusMonths`, and the two differ by up to two days on exactly the months a user would notice.

### D3. `SummariseUsageHistory`: a pure domain function

`domain/history/SummariseUsageHistory.kt`, constructed with a `Clock`, invoked with the period, the medicine's doses in the window and the medicine's earliest recorded moment. It returns a `UsageHistory`:

```
UsageHistory(
  period, firstDay, lastDay,
  scheduled, taken, skipped, missed, unanswered,
  recordsStartOn: LocalDate?,   // set only when later than firstDay
  buckets: List<UsageBucket>,
)
UsageBucket(start: LocalDate, isWeek: Boolean, scheduled, taken, skipped, missed, unanswered)
```

Counting rules, fixed here and in the spec:

- A dose counts when its scheduled moment is in the window. Future doses are not counted at all — not as scheduled, not as anything.
- `taken`, `skipped` and `missed` come from the recorded outcome. A past dose with no outcome yet is `unanswered`; it is a real state (the dose has not lapsed yet) and is neither a success nor a failure.
- `scheduled` is the sum of the four. Adherence is `taken / scheduled`, rounded to the nearest whole percent, and is **absent** when `scheduled` is zero rather than shown as 0 %.
- `recordsStartOn` is the local date of the medicine's earliest stored dose when that is later than the window's first day, otherwise null.

No Android imports, so every rule above is a plain unit test.

### D4. Buckets: by day for a week, by week for longer periods

`WEEK` produces 7 day buckets. `MONTH` and `THREE_MONTHS` produce week buckets aligned to the first day of the week for the device locale, which means the earliest bucket may be partial; its `start` is the window's first day, not the notional week start, so a label never claims data it does not have. That gives 7, about 5, and 13–14 bars.

Alternative considered: day buckets for the month period too, for a denser "contribution graph" texture. Rejected — 31 bars across a 328 dp content width leaves about 3 dp per bar once the gap is on the 4 dp grid: below the grid, invisible at a glance, and impossible to label. One rule — the week period is by day, longer periods are by week — is also simpler to describe than a per-period table.

### D5. The chart

`UsageChart` is a `Row` of equal-weight columns with `Arrangement.spacedBy(Spacing.xs)`, fixed height `Sizes.usageChartHeight` (96 dp, a new token). Each column is a stack drawn from the bottom whose total height is the bucket's scheduled count as a fraction of the largest bucket's scheduled count in the period; the space above it is the card surface. Segments, bottom up:

| Segment | Colour role | Why |
| --- | --- | --- |
| Taken | `primaryContainer` | Design system 2.3, taken |
| Skipped | `surfaceContainerHighest` | 2.3, skipped: neutral grey on purpose |
| Unanswered | `secondaryContainer` | 2.3, due |
| Missed | `errorContainer` | 2.3, missed |

No new hue, no hex, everything read from `MaterialTheme.colorScheme` through the existing `intakeStatusColors`. Missed sits on top because that is where the eye lands. The stacked portion is clipped to `MaterialTheme.shapes.small` and carries a 1 dp `outlineVariant` border, so the skipped segment stays legible where its tier sits next to the card's own.

Below the row, the first and last bucket dates as `bodySmall` in `onSurfaceVariant`, left and right of a `SpaceBetween` row. No per-bar labels: they cannot survive 200 % font scale at 13 bars.

Accessibility: each column is its own semantics node with a content description that states the bucket and the counts — "9 September, 2 of 2 doses taken", "week of 8 September, 12 of 14 doses taken", "9 September, no doses scheduled". The chart is therefore fully readable with TalkBack without the visual, and the summary above restates the totals in text regardless.

Alternative considered: a `Canvas`-drawn chart. Rejected — layout composables give wrapping, theming, right-to-left and per-bar semantics for nothing, and this chart has no curve to draw.

### D6. The screen

`ui/medicines/history/MedicineHistoryScreen.kt`, a `Scaffold` with a `TopAppBar` whose title is "Usage history" in `titleLarge` with an `ic_arrow_back` navigation icon (design system 8.7), content in one `verticalScroll` `Column` capped at `Spacing.contentMaxWidth`, in this order:

1. The **medicine name** in `headlineSmall` with `semantics { heading() }` — the app bar title is the fixed screen name, so the content says whose history this is and can wrap instead of truncating.
2. The **period selector**: `SingleChoiceSegmentedButtonRow`, full width, three `SegmentedButton`s labelled 1 week, 1 month, 3 months, `labelLarge`, minimum height `Sizes.minTouchTarget`. Material's selected segment already uses `secondaryContainer`, which is the design system's "selected" role.
3. The **summary card** (`tileContainerColor()`, `large` shape, `Spacing.lg` padding): the adherence percentage in `displaySmall` with tabular figures, a `bodyMedium` caption beneath it, and the scheduled and taken counts as two `titleSmall` numbers with `bodySmall` labels.
4. The **breakdown**: one full-width `full`-shaped horizontal bar 12 dp tall split in proportion between the four categories, then a legend with one row per non-zero category carrying that category's icon at `Sizes.iconChip`, its label and its count. The icon and label mean state never rests on colour alone.
5. The **chart card**: a `titleSmall` header naming the bucketing ("By day" / "By week"), the chart, the date labels, and the records-start note in `bodySmall` when `recordsStartOn` is set.

When `scheduled` is zero the summary, breakdown and chart are replaced by the design system 8.10 empty state — `ic_history` at `Sizes.iconEmptyState` in `onSurfaceVariant`, a `headlineSmall` line, a `bodyLarge` hint — while the period selector stays, so widening the period is the obvious next move.

`displayLarge` is not used anywhere on the screen: it belongs to Home's title, and this is a secondary screen. `displaySmall` for the one headline figure is the largest role the design system allows below it.

### D7. Route inside the medicine form graph

`@Serializable data class MedicineHistory(val medicationId: Long)` in `ui/navigation/Routes.kt`, registered as a `composable<MedicineHistory>` **inside** the `navigation<MedicationFormGraph>` block in `MedicationFormNavigation.kt`.

Inside the graph rather than beside it because the history only exists as a thing you opened from a medicine you have open: closing the flow with the existing `popBackStack(MedicationFormGraph, inclusive = true)` clears it too, and the form's draft — including unsaved edits — is still on the back stack when the user comes back. It is not a top-level destination, so `PillsnerApp` hides the bottom navigation on it for free.

The history screen takes its own view model from its **own** back-stack entry, not the shared graph entry, so it never touches the form's draft. The medication id travels in the route and arrives through `createSavedStateHandle()`, as `MedicationFormViewModel` already does.

The overflow menu is available even with unsaved edits, and opening it asks nothing: navigating forward is not leaving the form, so the discard dialog is not involved.

### D8. The overflow menu

The details top bar gains an `actions` slot with an `IconButton` (`ic_more_vert`, content description "More options", `Sizes.minTouchTarget`) opening a `DropdownMenu` with exactly one `DropdownMenuItem`: "Usage history". The menu is present only in edit mode — `uiState.showsActiveSwitch` already distinguishes the two modes — because an unsaved medicine has no history.

The menu holds one item and will hold nothing destructive: `medicine-details` forbids a delete, remove or archive action anywhere on the screen including its menus, and this change's delta restates that now that a menu exists, so the constraint is not quietly lost.

Alternative considered: a "Usage history" row inside the form. Rejected — the form is about editing, and a navigation row in the middle of a draft invites a tap that loses work. The overflow is where Android users look for a screen's secondary actions.

### D9. View model

`MedicineHistoryViewModel(medicationRepository, doseRepository, summarise, savedStateHandle, clock)`:

- reads `medicationId` from the route, loads the medicine once for its name, and emits `OpenFailed` when it cannot be read — the same treatment the form gives an unknown identifier;
- holds the selected period in the `SavedStateHandle`, defaulting to `WEEK`, so rotation and process death keep it;
- resolves the window from the period **once per selection** and `flatMapLatest`es onto `observeHistoryFor` for that fixed window, so an answer recorded while the screen is open re-emits without the window sliding under the user mid-read;
- maps to `MedicineHistoryUiState` through `SummariseUsageHistory`, in `stateIn` with `WhileSubscribed`.

Nothing on this screen writes, so there are no events beyond the period choice and back.

### D10. Strings, drawables and package layout

New keys in `values/strings.xml` and `values-nl/strings.xml`: the screen title, the three period labels, the adherence caption, the scheduled and taken labels, the four category labels, the "By day" and "By week" headers, the chart bar descriptions (with placeholders for the date and the two counts), the no-doses-in-bucket description, the records-start note, the empty-state title and hint, the menu item and the overflow content description. Percentages are formatted with the app locale through `NumberFormat`, and dates through `DateTimeFormatter` with a localised style — never by hand.

Two new Material Symbols Rounded drawables: `ic_more_vert` and `ic_history`.

```
domain/model/UsageHistory.kt          UsagePeriod, UsageHistory, UsageBucket
domain/history/SummariseUsageHistory.kt
ui/medicines/history/MedicineHistoryScreen.kt
ui/medicines/history/MedicineHistoryViewModel.kt
ui/medicines/history/MedicineHistoryUiState.kt
ui/medicines/history/UsageChart.kt
ui/medicines/history/UsageBreakdown.kt
```

## Risks / Trade-offs

- **The record only reaches back as far as the app has been running, so a three-month view of a medicine added last week looks empty** → `recordsStartOn` and its note on the screen say so in words. Without it a user would read thin data as poor adherence, which is the one misreading this screen must not cause.
- **A dose that has passed but not yet lapsed counts as "unanswered", and moves to "missed" later without the user doing anything** → It is a real, short-lived state and is labelled as itself rather than folded into missed. The counts are a live view of the record, not a frozen report, and the screen re-emits when the state settles.
- **Week buckets make the month view coarse** → Accepted, with the reason recorded in D4: day bars at that width fall below the spacing grid. A denser month view needs a different visual, and that is a proposal.
- **The two new queries scan one medicine's doses over up to three months** → A few hundred rows at most, served by the existing `(medication_id, scheduled_at)` index, read off the main thread through Room's `Flow`. No index to add.
- **`displaySmall` for the adherence figure is Raleway 200 at 34 sp** → Within the design system's rule that weight 200 is used only at 34 sp and above, and it is the one large figure on the screen, so no Raleway headers stack.
- **A menu on the details screen is a place a future change could put "Delete"** → The `medicine-details` delta states the prohibition for the menu explicitly, and the existing no-removal test is extended to walk the menu.

## Migration Plan

Additive and reversible. No schema change, no database version bump, no migration, and no persisted setting beyond the period held in saved state, which is process-scoped. Rolling back is deleting the new package and the two domain files and reverting four call sites: the form's top bar, the flow's navigation graph, `Routes.kt` and `AppContainer`.

## Open Questions

None.
