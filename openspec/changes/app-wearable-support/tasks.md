## 1. Preconditions

- [ ] 1.1 Verify `app-medicine-alarm` is applied (`DoseRepository.observePending()`, `ReminderCoordinator` in `AppContainer`) and `app-settings-language` is applied (localised context provider, supported-language list, `MissingTranslation` as error). Stop if either is missing
- [ ] 1.2 Confirm `app-medicine-alarm` will be archived before this change so the `medicine-reminders` MODIFIED delta resolves; otherwise rewrite it as ADDED with a distinct name before archiving
- [ ] 1.3 Check the current Play services Wearable documentation that data items travel only over the direct Bluetooth or local network link between paired devices; record the finding and the documentation date in the design's D3 section

## 2. Build setup

- [ ] 2.1 Add to `gradle/libs.versions.toml`, after re-checking the release pages for newer stable versions: `play-services-wearable` 20.0.1, `wear-compose-material3` 1.6.2, `wear-compose-foundation` 1.6.2, `wear-compose-ui-tooling` 1.6.2, `wear-tooling-preview` 1.0.0; confirm `kotlinx-serialization-json` is present from the scaffold; record the versions actually used in design D9
- [ ] 2.2 Create the `:shared` module (`kotlin("jvm")` with the JDK 21 toolchain plus the serialization plugin) and include it in `settings.gradle.kts`
- [ ] 2.3 Create the `:wear` module: Android application plugin, Kotlin Android, Kotlin Compose plugin, `applicationId` identical to `:app`, `minSdk` 30, `compileSdk`/`targetSdk` 37 matching `:app`, the same Compose BOM, dependencies on `:shared`, Wear Compose Material 3 and foundation, Play services Wearable, lifecycle view model; include it in `settings.gradle.kts`
- [ ] 2.4 Share the debug signing config between `:app` and `:wear` so debug builds pair on emulators; document release signing expectations in the README build section without committing any keystore
- [ ] 2.5 Add `play-services-wearable` to `:app`; confirm neither manifest declares the internet permission

## 3. Shared contract

- [ ] 3.1 Create `shared/.../WearSyncContract.kt` with the data path and the JSON key
- [ ] 3.2 Create `shared/.../SyncedDoses.kt` with `SyncedDose(doseId, medicationName, amountText, scheduledAtEpochMillis)` and `SyncedDoses(version = 1, languageTag, publishedAtEpochMillis, doses)`, a `Json` instance with `ignoreUnknownKeys = true`, and `encode`/`decode` helpers that return null for a higher major version
- [ ] 3.3 Unit test the contract: round trip, unknown field ignored, higher version returns null

## 4. Phone publisher

- [ ] 4.1 Create `app/.../data/wear/WearDataClientFactory.kt` that returns a `DataClient` only when `GoogleApiAvailability` reports success, otherwise null
- [ ] 4.2 Create `app/.../data/wear/DoseSyncPublisher.kt`: collect `observePending()`, debounce 500 ms, map to `SyncedDoses` (amounts through `QuantityFormatter` under the localised context, language tag from the language provider, ordered by time), `putDataItem` urgent at the contract path; no-op when the client is null; debug-level logging with counts only
- [ ] 4.3 Start the publisher from `ReminderCoordinator` (application scope) at app start, and trigger a republish after every `onWake` and after a language change
- [ ] 4.4 Unit test the publisher with a fake data client: dose taken removes it, new medicine adds its doses, burst produces one publish, Dutch language yields "2 tabletten", null client publishes nothing

## 5. Watch data layer

- [ ] 5.1 Create `wear/.../data/UpcomingDosesRepository.kt` exposing `Flow<SyncedDoses?>` from the persisted data item plus `DataClient` listener updates while collected
- [ ] 5.2 Create `wear/.../data/WearDataListenerService.kt` and declare it in the manifest with a `DATA_CHANGED` filter for the contract path; the service body only lets the item persist
- [ ] 5.3 Create `wear/.../data/PhoneConnectivity.kt` wrapping `NodeClient.connectedNodes` as a suspend check
- [ ] 5.4 Create `WearContainer` (manual DI, mirroring the phone's `AppContainer`) built in the wear `Application` class

## 6. Watch domain and view model

- [ ] 6.1 Create `wear/.../domain/UpcomingWindowFilter.kt` (Android-free): keep doses with `scheduledAt <= now + 6h`, ordered ascending, overdue ones first by time
- [ ] 6.2 Create `wear/.../ui/WatchUiState.kt` and `WatchViewModel.kt` combining the repository flow, a minute ticker aligned to the minute boundary, and the connectivity check into `WatchUiState(entries, isEmpty, phoneConnected, hasData, locale)`
- [ ] 6.3 Unit test the filter and view model: inside window, boundary at exactly six hours, overdue first, entering the window on tick, empty state, `hasData` false before first payload, higher-version payload treated as absent

## 7. Watch UI

- [ ] 7.1 Add wear string resources in `values/` and `values-nl/`: app name, header "Next 6 hours", empty state "No medicines scheduled for the upcoming 6 hours", "Tomorrow" prefix, footer "Phone not connected", footer "Open Pillsner on your phone to sync", entry content description template; enable the `MissingTranslation` error for the module
- [ ] 7.2 Create `wear/.../ui/theme` with a Wear Material 3 `ColorScheme` from the design system's dark palette (section 2.2 dark column, the only hex literals in the module), an intake status colour mapping per section 2.3 (due `secondaryContainer`, overdue `errorContainer`), and Wear Material 3 default typography and shapes; no dynamic colour
- [ ] 7.3 Create `wear/.../ui/LocalizedContent.kt` that derives a configuration context from the payload language tag (or the system locale when absent) and provides it to the content
- [ ] 7.4 Create `wear/.../ui/DoseCard.kt`: name, amount and time (with tomorrow indication), status icon and colour per section 2.3 (due or overdue), one merged semantics node, minimum 48 dp height
- [ ] 7.5 Create `wear/.../ui/UpcomingDosesScreen.kt`: `AppScaffold` with `TimeText`, `ScreenScaffold`, `TransformingLazyColumn` with header, cards or empty state, and the footer item when the phone is disconnected; rotary scrolling enabled; test tags for header, cards, empty state and footer
- [ ] 7.6 Create `MainActivity` and `WearApp` wiring the view model from `WearContainer`; add previews for round and square screens for the list, the empty state and the disconnected state
- [ ] 7.7 Confirm the wear manifest declares the watch feature, `standalone = false`, the listener service, and does not set the notification bridge mode

## 8. Tests

- [ ] 8.1 Compose tests on a Wear emulator: list with three entries in order, entry content description, empty state text exact, footer shown when disconnected, footer text when never synced, Dutch rendering when the payload language is `nl`
- [ ] 8.2 Instrumented test for `UpcomingDosesRepository` with a locally written data item: initial read and listener update
- [ ] 8.3 Lint or unit scan of `data/wear` in `:app` and the whole `:wear` module for info-or-higher log statements containing string templates
- [ ] 8.4 Document manual test cases in the change: pair a phone and watch emulator and see the list appear; take a dose on the phone and watch it disappear; disable Bluetooth and see the footer; reopen the watch app offline and see the last list; switch the phone app to Dutch and see the watch follow; largest watch font; round and square devices; a due dose still produces the bridged notification with three actions while the watch app is installed

## 9. Verification and documentation

- [ ] 9.1 Run `./gradlew test` and `./gradlew lint` for `:app`, `:wear` and `:shared` from `src/`; fix failures and report results verbatim
- [ ] 9.2 Run `./gradlew :wear:connectedAndroidTest` on a Wear emulator and `./gradlew :app:connectedAndroidTest` for the unchanged phone suites
- [ ] 9.3 Update `README.md`: technology table gains "Wearable: Wear OS companion app with Compose for Wear OS" and "Phone to watch sync: Wearable Data Layer (Play services)"; repository layout lists the `wear` and `shared` modules; "Features" gains the watch list; a privacy note states what is synced to the watch and that it stays between the two devices
- [ ] 9.4 Review against `CLAUDE.md`: no network permission, dependency justified and disclosed, strings in resources with translations, no sensitive logging, single DI mechanism per app, version catalog, glossary terms
