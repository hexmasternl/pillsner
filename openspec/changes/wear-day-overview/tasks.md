## 1. The sync contract

- [x] 1.1 Add `SyncedMedicineDetails` (default dose, schedule lines, stock) to the shared module and an optional `details` field to `SyncedDose`, leaving `CURRENT_VERSION` where it is
- [x] 1.2 Extend `SyncedDosesTest` with a round trip carrying details, a payload from an older phone without them, and a medicine with no stock line

## 2. The phone publishes the medicine behind each dose

- [x] 2.1 Add `WearMedicineDetails`, reading the medicine and its stock batches and formatting the default dose, the schedule lines and the stock total with the phone's own formatters
- [x] 2.2 Give `DoseSyncPublisher` a `medicineDetails` lookup, resolved once per medicine per publish, and attach the result to each dose
- [x] 2.3 Wire both into `AppContainer` with the app-language quantity and schedule formatters
- [x] 2.4 Add `WearMedicineDetailsTest` and extend `DoseSyncPublisherTest`

## 3. The watch agenda

- [x] 3.1 Replace `UpcomingWindowFilter` with `DayAgenda`: today and tomorrow in the watch's zone, grouped by the minute each dose is due
- [x] 3.2 Reshape `WatchUiState` into day sections of time groups, carrying the medicine details per dose, and map to it in `WatchViewModel`
- [x] 3.3 Render the agenda as day headings, time headings and dose cards in one scrolling `TransformingLazyColumn`, with the time off the card and in its heading
- [x] 3.4 Reword the empty state and add the day headings to all six languages
- [x] 3.5 Replace `UpcomingWindowFilterTest` with `DayAgendaTest` and update `WatchViewModelTest` and `UpcomingDosesScreenTest`

## 4. The read-only dose details

- [x] 4.1 Add `DoseDetailsScreen`: the dose's amount and time, the medicine's default dose, schedule and stock, and a line saying so when the phone sent no details
- [x] 4.2 Make a dose card clickable, with a "Show details" click label, and open the details from `WearApp` with `BackHandler` returning to the agenda
- [x] 4.3 Add the details strings in all six languages
- [x] 4.4 Add `DoseDetailsScreenTest`, including that nothing on the screen writes anything

## 5. Documentation

- [x] 5.1 Update the watch section of `README.md` and the sync disclosure in `README.md` and `PRIVACY.md`

## 6. Verification

- [ ] 6.1 Run the unit tests and lint from `src/`
- [ ] 6.2 Run the watch instrumented tests
- [ ] 6.3 Check the watch screens against `docs/design-system.md`: theme tokens only, string resources only, minimum touch targets, readable at the largest watch font size on round and square screens
