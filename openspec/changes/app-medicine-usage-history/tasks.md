## 1. Scaffold check

- [x] 1.1 Confirm the Gradle project, the `app` module, `AppContainer`, the `ui/theme` package and the `ui/medicines/form` package all exist in `src/`. Stop and report if any of them does not; this change creates none of them.
- [x] 1.2 Confirm `docs/design-system.md` is the version this change was designed against and read sections 2.3, 3.2, 4, 5, 8.3, 8.7, 8.10 before writing any UI.

## 2. Domain model and summariser

- [x] 2.1 Add `domain/model/UsageHistory.kt` with `UsagePeriod` (WEEK, MONTH, THREE_MONTHS), `UsageBucket` and `UsageHistory` exactly as design D3 lists them. No Android imports; KDoc the counting rules on each count.
- [x] 2.2 Give `UsagePeriod` a `window(today: LocalDate): ClosedRange<LocalDate>` following design D2: six days back, one month back plus a day, three months back plus a day.
- [x] 2.3 Add `domain/history/SummariseUsageHistory.kt`: takes the period, the doses in the window and the medicine's earliest recorded instant; returns `UsageHistory`. Implement the counting rules of D3 — future doses excluded, past doses with no outcome counted as unanswered, `scheduled` the sum of four, adherence absent when `scheduled` is zero.
- [x] 2.4 Implement bucketing per design D4: day buckets for WEEK, locale-week buckets for MONTH and THREE_MONTHS, the earliest bucket starting on the window's first day when it falls mid-week.
- [x] 2.5 Unit-test the window arithmetic: a plain week, a month ending on the 31st, three months across a leap day, and a window spanning a daylight-saving transition in both directions.
- [x] 2.6 Unit-test the counting rules: the four outcomes counted separately, a dose later today excluded, tomorrow excluded, the day before the window excluded, a skipped dose never counted as missed, adherence rounding (2 of 3 is 67 %), and zero scheduled yielding no adherence.
- [x] 2.7 Unit-test bucketing: seven buckets for a week with the last being today, week-aligned buckets for a month and three months with a partial first bucket, empty buckets preserved in place, and bucket totals summing to the period totals.
- [x] 2.8 Unit-test `recordsStartOn`: set when the earliest stored dose is later than the window's first day, null when the records reach further back, and null for a medicine with gaps but older records.

## 3. Data layer

- [x] 3.1 Add `observeHistoryFor(medicationId, from, to): Flow<List<Dose>>` and `earliestScheduledAt(medicationId): Instant?` to `domain/repository/DoseRepository.kt`, with KDoc saying both are read-only and that history is never withdrawn.
- [x] 3.2 Add the two queries of design D1 to `data/db/DoseDao.kt`. Verify no `DoseEntity` change is needed and that `PillsnerDatabase`'s version and migration list stay untouched.
- [x] 3.3 Implement both in `data/RoomDoseRepository.kt` and in `data/InMemoryDoseRepository.kt`.
- [x] 3.4 Add DAO tests in `DoseDaoTest`: the range query returns only the given medicine's doses inside the range, in scheduled order, including answered and unanswered ones; `earliestScheduledAt` returns the oldest moment and null for a medicine with no doses.
- [x] 3.5 Confirm `PillsnerDatabaseMigrationTest` still passes unchanged, proving no schema change slipped in.

## 4. Strings, drawables and theme tokens

- [ ] 4.1 Add `ic_more_vert.xml` and `ic_history.xml` as Material Symbols Rounded 24 dp vectors in `app/src/main/res/drawable/`, matching the style of the existing icons.
- [ ] 4.2 Add `Sizes.usageChartHeight = 96.dp` and the breakdown bar height to `ui/theme/Dimens.kt` with a comment pointing at design D5. Add no other new dimension.
- [ ] 4.3 Add every new key of design D10 to `values/strings.xml`, grouped under a "Medicine usage history" comment, with placeholder comments on the formatted ones.
- [ ] 4.4 Add the Dutch translations to `values-nl/strings.xml` and confirm `TranslationCompletenessTest` passes.

## 5. Usage history screen

- [ ] 5.1 Add `ui/medicines/history/MedicineHistoryUiState.kt`: the medicine name, the selected period, the `UsageHistory`, a loading flag, and an `OpenFailed` effect.
- [ ] 5.2 Add `ui/medicines/history/MedicineHistoryViewModel.kt` per design D9: identifier and period from the `SavedStateHandle`, medicine loaded once for its name, `flatMapLatest` from the fixed window onto `observeHistoryFor`, summarised into state with `stateIn(WhileSubscribed)`. Log the open failure without the medicine name or any amount.
- [ ] 5.3 Add `ui/medicines/history/UsageBreakdown.kt`: the proportional bar and the legend of design D6 item 4, colours through `intakeStatusColors`, every row carrying an icon, a label and a count.
- [ ] 5.4 Add `ui/medicines/history/UsageChart.kt` per design D5: equal-weight columns, stacked bottom-up taken/skipped/unanswered/missed, heights relative to the largest bucket, `small` clip with an `outlineVariant` border, first and last bucket dates beneath, one semantics node per bar with the full description.
- [ ] 5.5 Add `ui/medicines/history/MedicineHistoryScreen.kt` per design D6: top app bar, medicine name heading, segmented period row, summary card, breakdown, chart card, records-start note, and the 8.10 empty state with the period row still visible. Every colour, size and text style from a token; no inline string.
- [ ] 5.6 Add `@PreviewLightDark` previews for the screen (a full week, a sparse three months, the empty state) and for the chart and the breakdown on their own.
- [ ] 5.7 Register `MedicineHistoryViewModel` in `AppContainer.viewModelFactory` with the summariser and the clock.

## 6. Entry point and navigation

- [ ] 6.1 Add `@Serializable data class MedicineHistory(val medicationId: Long)` to `ui/navigation/Routes.kt`.
- [ ] 6.2 Add the overflow action and its one-item `DropdownMenu` to `MedicationFormScreen`'s top bar per design D8, shown only when `uiState.showsActiveSwitch`, with a test tag for the action and the item, and a content description on the action.
- [ ] 6.3 Register `composable<MedicineHistory>` inside the `navigation<MedicationFormGraph>` block in `MedicationFormNavigation.kt`, taking the view model from its own entry, and wire the menu item to navigate to it with the open medicine's identifier.
- [ ] 6.4 Handle the `OpenFailed` effect by popping back and showing the existing "Could not open medicine" message.
- [ ] 6.5 Confirm `PillsnerApp` hides the bottom navigation on the new destination without changing anything there.

## 7. UI tests

- [ ] 7.1 Compose test: the overflow action is absent in add mode, present in edit mode, and its menu holds exactly one item.
- [ ] 7.2 Extend `NoMedicineDeletionTest` to open the overflow menu and assert that no delete, remove or archive action exists in it.
- [ ] 7.3 Compose test: the history screen shows the medicine name, the title, the three segments with "1 week" selected, and the scheduled, taken and adherence figures for a known fixture.
- [ ] 7.4 Compose test: selecting "3 months" recomputes the figures and the chart; the selection survives rotation.
- [ ] 7.5 Compose test: the empty state appears when nothing is scheduled and the segments remain tappable; widening the period replaces it with the figures.
- [ ] 7.6 Compose test: the records-start note appears when the medicine is younger than the period and not otherwise.
- [ ] 7.7 Compose test: each bar's content description reads the bucket and the "n of m taken" counts, including the no-doses-scheduled wording.
- [ ] 7.8 Navigation test: open the history from the details overflow, press back, and assert the form is shown with an earlier edit still present and no discard dialog.

## 8. Design review and verification

- [ ] 8.1 Run the `pillsner-ui-review` skill over `ui/medicines/history/` and the changed top bar; fix every violation it reports.
- [ ] 8.2 Check the screen by eye at 100 %, 150 % and 200 % font scale in both themes: nothing truncates, the chart still reads, the whole screen scrolls.
- [ ] 8.3 Run the unit tests and lint from `src/`; report any failure verbatim.
- [ ] 8.4 Run the instrumented tests, since the DAO and the navigation graph changed.
- [ ] 8.5 Re-read the three spec files and confirm every requirement has a test or a documented manual check behind it.
