## 1. Domain and persistence

- [ ] 1.1 Add a `critical: Boolean` field (default `false`) to the `Medication` and `NewMedication` domain models
- [ ] 1.2 Add a `critical` column to the Room `Medication` entity with a Room migration and a migration test
- [ ] 1.3 Wire the flag through the `MedicationRepository` add and update operations, in-memory implementation included
- [ ] 1.4 Add `critical: Boolean` to `Dose` and `PlannedDose`, snapshotted from the medicine the same way `medicationName` and `amount` already are; populate it in `DoseGenerator.plan`
- [ ] 1.5 Extend `RefreshPlannedDoses`' existing snapshot refresh (`doseRepository.refreshSnapshots`) to keep a pending dose's `critical` snapshot following its medicine, the same way `medicationName`/`amount` already do

## 2. Add and details forms

- [ ] 2.1 Add the "This is a critical medicine" toggle to the shared medicine form composable, off by default
- [ ] 2.2 Wire the toggle to the Add medicine form's save path
- [ ] 2.3 Pre-populate and persist the toggle on the Medicine details form's save path
- [ ] 2.4 Confirm string resources for the toggle label exist in every supported language

## 3. Critical badge in the UI

- [ ] 3.1 Design the critical badge (icon + "Critical" text) with the `pillsner-designer` agent, following `docs/design-system.md`
- [ ] 3.2 Show the badge on medicine tiles in the Medicines screen, with non-colour-alone distinction and screen-reader announcement
- [ ] 3.3 Show the badge on the dose detail screen, reading `dose.critical` from the `Dose` already streamed by `DoseDetailViewModel` (no new `MedicationRepository` lookup)
- [ ] 3.4 Run `pillsner-ui-review` on every changed composable

## 4. Reminder escalation

- [ ] 4.1 Create the `critical_reminder` notification channel with its distinct sound and vibration pattern, alongside the existing channel, at app start
- [ ] 4.2 Route notification posting to the critical channel when `dose.critical` is set, reading it from the `Dose` `ReminderNotifier.show(dose, ...)` already receives (no new lookup)
- [ ] 4.3 Make the snooze interval and the repeat interval/count read from `dose.critical` (15 min / 4 repeats standard, 5 min / 8 repeats critical) wherever they are currently hard-coded in the alarm-arming and re-ask logic, computed fresh at each arming so a flag toggled after planning but before the first reminder takes effect
- [ ] 4.4 Confirm the repeat-reset-on-snooze behaviour applies unchanged under both cadences

## 5. Wearable sync

- [ ] 5.1 Add `critical: Boolean` to the shared sync contract payload's dose type, defaulting to `false` when absent on deserialisation
- [ ] 5.2 Publish `dose.critical` from the phone for every pending dose in the existing `DoseRepository.observePending()` → payload mapping (no new join; the flag now travels on `Dose` itself)
- [ ] 5.3 Show the critical badge on the watch list entry

## 6. Tests

- [ ] 6.1 Unit tests: `Medication`/`NewMedication` construction and repository round-trip with the critical flag
- [ ] 6.2 Unit tests: cadence selection (interval and repeat count) picks the critical values only when `dose.critical` is set, including the snooze-resets-repeats case under both cadences
- [ ] 6.3 Unit test: `DoseGenerator.plan` snapshots the medicine's current `critical` flag onto each `PlannedDose`
- [ ] 6.4 Unit test: `RefreshPlannedDoses` updates a pending dose's stored `critical` snapshot when the medicine's flag changes, the same way it already does for `medicationName`/`amount`
- [ ] 6.5 Room migration test for the new column
- [ ] 6.6 DAO test exercising a query that reads or filters on the critical flag, if one is added
- [ ] 6.7 Compose semantics tests: critical badge present/absent and screen-reader announcement on the Medicines screen, dose detail screen and medicine forms
- [ ] 6.8 Wearable sync contract round-trip test including the critical field and the missing-field-defaults-false case
- [ ] 6.9 Instrumented test: a critical dose's reminder is armed on the critical channel with the 5-minute repeat cadence

## 7. Verification

- [ ] 7.1 Run unit tests and lint from `src`
- [ ] 7.2 Run instrumented tests (alarm scheduling and notification channel changed)
- [ ] 7.3 Confirm `docs/design-system.md` needs no update, or update it if the badge introduces a new token
