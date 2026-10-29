# Tasks

## 1. Failing tests first

- [ ] 1.1 Write the failing unit tests that show the reported problem: in a new `CorrectIntakeTest` (`src/app/src/test/.../domain/intake/`), a missed dose corrected with "I took it" ends up taken at its scheduled moment, and a taken dose corrected with "I didn't take it" ends up skipped. Use made-up medicine names. Verify they fail on `development` because no correction path exists (compile failure against a stubbed `CorrectIntake` that does nothing counts).

## 2. Schema: corrected marker and stock draws (room-schema-change skill)

- [ ] 2.1 Add `corrected_at` to `DoseEntity` and a `StockDrawEntity`/`StockDrawDao` for `stock_draws(dose_id, batch_id, amount_value, amount_unit)`, primary key `(dose_id, batch_id)`, both foreign keys `ON DELETE CASCADE`, indexed on `batch_id`. Verify `./gradlew :app:kspDebugKotlin` succeeds.
- [ ] 2.2 Write `MIGRATION_6_7`, add it to `Migrations.ALL`, bump `PillsnerDatabase.VERSION` to 7 and commit the exported `schemas/.../7.json`. Verify the new `PillsnerDatabaseMigrationTest` case passes: v6 data with taken, skipped and missed doses and stock batches survives, `corrected_at` is null and `stock_draws` is empty.
- [ ] 2.3 Add the guarded `DoseDao.correctIntake(id, expected, outcome, recordedAt, correctedAt): Int` (`WHERE id = ? AND outcome = ?`), `StockBatchDao.addToRemaining(batchId, amount)` and the draw queries. Verify with `DoseDaoTest` (a stale expected outcome updates 0 rows) and `StockBatchDaoTest`/a new `StockDrawDaoTest` (removing a batch or purging a dose drops its draws).
- [ ] 2.4 Confirm app reset and the history purge leave no orphaned draws, and that the backup export still writes format 1 unchanged. Verify with the existing reset, retention and backup tests plus one assertion each on `stock_draws`.

## 3. Domain: record draws, correct intakes

- [ ] 3.1 Add `correctedAt: Instant?` (default null) to `Intake` and map it through `RoomDoseRepository` and `InMemoryDoseRepository`. Verify the existing unit tests still pass.
- [ ] 3.2 Make `consumeFefo`'s result expose what each batch gave (including whole-pill round-up), and make `ConsumeStockOnTaken.deduct` take the dose id and record the draws in the same transaction. Verify with `FefoConsumptionTest` (spanning, exact-fit and whole-pill cases report their draws) and `ConsumeStockOnTakenTest`/`AnswerDoseTest` (a reminder take writes draws, and skip/snooze write none).
- [ ] 3.3 Add `DoseRepository.correctIntake` and the draw operations to the domain repository contracts, with KDoc on the compare-and-set behaviour. Verify both repository implementations compile and the in-memory one is covered by `CorrectIntakeTest`.
- [ ] 3.4 Implement `CorrectIntake(doseId, choice, expected)`. It checks the 14-day local-date window, maps the choice (I took it → TAKEN at the scheduled moment; I didn't take it → SKIPPED at now), does the guarded update, returns or deducts stock in one transaction, flags the stock warning after commit for a correction to taken, and returns `Corrected | Unchanged | Changed`. Verify the 1.1 tests now pass, plus new cases:
  - window edges at local midnight and across a DST change
  - not answered yet is refused
  - stale expected → `Changed`
  - same outcome → `Unchanged` and not marked
  - stock returned per draw
  - removed batch and no-draw doses leave stock as-is
  - low-stock warning enqueued
  - missed rule leaves a corrected dose alone
- [ ] 3.5 Make `SummariseTimeDeviation` skip doses corrected to taken. Verify with a new `SummariseTimeDeviationTest`/`SummariseUsageHistoryTest` case: one late take plus one corrected take gives the late take's deviation over one dose, while the outcome counts include both.
- [ ] 3.6 Wire `CorrectIntake` in `AppContainer` without any reminder, notification or wear side effect. Verify by review and by a `CorrectIntakeTest` case asserting no `onAnswered`-style callback exists or fires.

## 4. UI: Recent doses and the correction sheet (pillsner-ui-build, app-strings)

- [ ] 4.1 Add the strings in all six locales through the `app-strings` skill: section title, 14-day caption, empty line, row texts (taken at / early / late / corrected), the "Change outcome" click label, the sheet title and options ("I took it", "I didn't take it"), "Save" and the "updated in the meantime" message. Verify `./gradlew :app:lintDebug` reports no MissingTranslation/ExtraTranslation.
- [ ] 4.2 Extend `MedicineHistoryViewModel`/`MedicineHistoryUiState` with the 14-day recent-doses list (its own `observeHistoryFor` collection, independent of the period) and the correction sheet state (open, choice, save, result), persisting the open sheet's dose id and choice in `SavedStateHandle`. Verify with `MedicineHistoryViewModelTest` cases: list order and content, independence from the period, Save enabled only on a change, the `Changed` result message, and the sheet restored after process death.
- [ ] 4.3 Build the Recent doses section and rows. Each row is one accessibility node with a "Change outcome" action when correctable, and none when not answered yet. Status chips reuse the intake state roles and icons, and there are previews in light, dark and the largest font. Verify with `MedicineHistoryScreenTest` semantics tests (rows, chips, taken time and difference, corrected label, unanswered row not clickable).
- [ ] 4.4 Build the correction `ModalBottomSheet` (`selectableGroup` with two radio rows, preselection rules, Save enabled only on a change, dismiss changes nothing). Verify with `MedicineHistoryScreenTest`: open from a skipped row, save "I took it", the row and the figures update, dismiss leaves the outcome, and a missed dose starts with no choice.
- [ ] 4.5 Run `pillsner-ui-review` on `ui/medicines/history/` and fix every finding. Verify the review reports no violations.

## 5. Website

- [ ] 5.1 Describe correcting a dose outcome on the website's usage-history/feature page and in the FAQ, in all six locales, through the `website-feature-sync` skill. Verify `hugo --minify --panicOnWarning` passes and every locale has the same structure as English.

## 6. Verification

- [ ] 6.1 Run `verify-change`: unit tests, lint and assembly, plus instrumented tests through `emulator-tests` (the migration, DAO, history screen and safety set, since database and stock code changed). Record the result in this file's Verification section. Verify every listed check passes, and report any failure verbatim.
