## 1. Preconditions

- [x] 1.1 Verify `app-medicine-alarm` is applied (`DoseRepository.observePending()`, `ReminderCoordinator` in `AppContainer`) and `app-settings-language` is applied (localised context provider, supported-language list, `MissingTranslation` as error). Stop if either is missing — all present
- [x] 1.2 Confirm `app-medicine-alarm` will be archived before this change so the `medicine-reminders` MODIFIED delta resolves; otherwise rewrite it as ADDED with a distinct name before archiving — archived as `2026-09-13-app-medicine-alarm`
- [x] 1.3 Check the current Play services Wearable documentation that data items travel only over the direct Bluetooth or local network link between paired devices; record the finding and the documentation date in the design's D3 section — recorded in D3, checked 13 September 2026, with the merged-manifest check beside it

## 2. Build setup

- [x] 2.1 Add to `gradle/libs.versions.toml`, after re-checking the release pages for newer stable versions: `play-services-wearable` 20.0.1, `wear-compose-material3` 1.6.2, `wear-compose-foundation` 1.6.2, `wear-compose-ui-tooling` 1.6.2, `wear-tooling-preview` 1.0.0; confirm `kotlinx-serialization-json` is present from the scaffold; record the versions actually used in design D9 — re-checked against Google's Maven metadata on 13 September 2026: 20.0.1 is still the newest stable play-services-wearable and Wear Compose 1.7.0 is still a release candidate, so the design's versions stand unchanged. `kotlinx-coroutines-play-services` 1.11.0 was added too, so the Task API reads as suspend functions
- [x] 2.2 Create the `:shared` module (`kotlin("jvm")` with the JDK 21 toolchain plus the serialization plugin) and include it in `settings.gradle.kts` — the Kotlin JVM plugin is declared `apply false` in the root build, because AGP 9 already carries the Kotlin plugin and a version request in the subproject conflicts with it
- [x] 2.3 Create the `:wear` module: Android application plugin, Kotlin Android, Kotlin Compose plugin, `applicationId` identical to `:app`, `minSdk` 30, `compileSdk`/`targetSdk` 37 matching `:app`, the same Compose BOM, dependencies on `:shared`, Wear Compose Material 3 and foundation, Play services Wearable, lifecycle view model; include it in `settings.gradle.kts`
- [x] 2.4 Share the debug signing config between `:app` and `:wear` so debug builds pair on emulators; document release signing expectations in the README build section without committing any keystore — no configuration was needed: both modules use AGP's default debug signing, which is the one keystore in the user's `.android` folder. Said so in the module comment and in the README
- [x] 2.5 Add `play-services-wearable` to `:app`; confirm neither manifest declares the internet permission — checked on the merged manifests: `:app` has only its four existing permissions, `:wear` has none at all

## 3. Shared contract

- [x] 3.1 Create `shared/.../WearSyncContract.kt` with the data path and the JSON key
- [x] 3.2 Create `shared/.../SyncedDoses.kt` with `SyncedDose(doseId, medicationName, amountText, scheduledAtEpochMillis)` and `SyncedDoses(version = 1, languageTag, publishedAtEpochMillis, doses)`, a `Json` instance with `ignoreUnknownKeys = true`, and `encode`/`decode` helpers that return null for a higher major version
- [x] 3.3 Unit test the contract: round trip, unknown field ignored, higher version returns null — `SyncedDosesTest`, 5 cases including malformed input

## 4. Phone publisher

- [x] 4.1 Create `app/.../data/wear/WearDataClientFactory.kt` that returns a `DataClient` only when `GoogleApiAvailability` reports success, otherwise null
- [x] 4.2 Create `app/.../data/wear/DoseSyncPublisher.kt`: collect `observePending()`, debounce 500 ms, map to `SyncedDoses` (amounts through `QuantityFormatter` under the localised context, language tag from the language provider, ordered by time), `putDataItem` urgent at the contract path; no-op when the client is null; debug-level logging with counts only — the `putDataItem` call sits behind a one-method `SyncTarget`, so what gets published is readable in a plain unit test
- [x] 4.3 Start the publisher from `ReminderCoordinator` (application scope) at app start, and trigger a republish after every `onWake` and after a language change — the first two as designed. A language change has no separate trigger, deliberately: the chosen language only takes effect when the app restarts (the `app-settings-language` rule), and the restart runs a wake, so the watch hears the new language exactly when the phone itself is in it. Publishing earlier would send a language the phone is not yet using
- [x] 4.4 Unit test the publisher with a fake data client: dose taken removes it, new medicine adds its doses, burst produces one publish, Dutch language yields "2 tabletten", null client publishes nothing — `DoseSyncPublisherTest`, 6 cases

## 5. Watch data layer

- [x] 5.1 Create `wear/.../data/UpcomingDosesRepository.kt` exposing `Flow<SyncedDoses?>` from the persisted data item plus `DataClient` listener updates while collected
- [x] 5.2 Create `wear/.../data/WearDataListenerService.kt` and declare it in the manifest with a `DATA_CHANGED` filter for the contract path; the service body only lets the item persist
- [x] 5.3 Create `wear/.../data/PhoneConnectivity.kt` wrapping `NodeClient.connectedNodes` as a suspend check
- [x] 5.4 Create `WearContainer` (manual DI, mirroring the phone's `AppContainer`) built in the wear `Application` class

## 6. Watch domain and view model

- [x] 6.1 Create `wear/.../domain/UpcomingWindowFilter.kt` (Android-free): keep doses with `scheduledAt <= now + 6h`, ordered ascending, overdue ones first by time
- [x] 6.2 Create `wear/.../ui/WatchUiState.kt` and `WatchViewModel.kt` combining the repository flow, a minute ticker aligned to the minute boundary, and the connectivity check into `WatchUiState(entries, isEmpty, phoneConnected, hasData, locale)`
- [x] 6.3 Unit test the filter and view model: inside window, boundary at exactly six hours, overdue first, entering the window on tick, empty state, `hasData` false before first payload, higher-version payload treated as absent — `UpcomingWindowFilterTest` (6 cases) and `WatchViewModelTest` (9 cases). The tick case is covered by the filter tests against a moving clock rather than by advancing virtual time through the view model's own ticker

## 7. Watch UI

- [x] 7.1 Add wear string resources in `values/` and `values-nl/`: app name, header "Next 6 hours", empty state "No medicines scheduled for the upcoming 6 hours", "Tomorrow" prefix, footer "Phone not connected", footer "Open Pillsner on your phone to sync", entry content description template; enable the `MissingTranslation` error for the module
- [x] 7.2 Create `wear/.../ui/theme` with a Wear Material 3 `ColorScheme` from the design system's dark palette (section 2.2 dark column, the only hex literals in the module), an intake status colour mapping per section 2.3 (due `secondaryContainer`, overdue `errorContainer`), and Wear Material 3 default typography and shapes; no dynamic colour — with a small `WearDimens` beside it, so no composable writes a raw dp value, as on the phone
- [x] 7.3 Create `wear/.../ui/LocalizedContent.kt` that derives a configuration context from the payload language tag (or the system locale when absent) and provides it to the content
- [x] 7.4 Create `wear/.../ui/DoseCard.kt`: name, amount and time (with tomorrow indication), status icon and colour per section 2.3 (due or overdue), one merged semantics node, minimum 48 dp height
- [x] 7.5 Create `wear/.../ui/UpcomingDosesScreen.kt`: `AppScaffold` with `TimeText`, `ScreenScaffold`, `TransformingLazyColumn` with header, cards or empty state, and the footer item when the phone is disconnected; rotary scrolling enabled; test tags for header, cards, empty state and footer — `TimeText` and rotary scrolling both come from `AppScaffold` and `TransformingLazyColumn` themselves; neither is configured by hand
- [x] 7.6 Create `MainActivity` and `WearApp` wiring the view model from `WearContainer`; add previews for round and square screens for the list, the empty state and the disconnected state
- [x] 7.7 Confirm the wear manifest declares the watch feature, `standalone = false`, the listener service, and does not set the notification bridge mode

## 8. Tests

- [x] 8.1 Compose tests on a Wear emulator: list with three entries in order, entry content description, empty state text exact, footer shown when disconnected, footer text when never synced, Dutch rendering when the payload language is `nl` — `UpcomingDosesScreenTest`, 7 cases, all passing. See 9.2 for the device they ran on
- [x] 8.2 Instrumented test for `UpcomingDosesRepository` with a locally written data item: initial read and listener update — `UpcomingDosesRepositoryTest`, 2 cases. They skip themselves (`assumeTrue`) where the Wearable API is unavailable, which is every device that is not a Wear OS one or a paired phone, and is the case here
- [x] 8.3 Lint or unit scan of `data/wear` in `:app` and the whole `:wear` module for info-or-higher log statements containing string templates — `WearSyncLoggingTest`, modelled on the reminders one: debug level only, and a log line may carry a count, a status or an error type, never a medicine
- [x] 8.4 Document manual test cases in the change: pair a phone and watch emulator and see the list appear; take a dose on the phone and watch it disappear; disable Bluetooth and see the footer; reopen the watch app offline and see the last list; switch the phone app to Dutch and see the watch follow; largest watch font; round and square devices; a due dose still produces the bridged notification with three actions while the watch app is installed — 12 cases in `manual-tests.md`

## 9. Verification and documentation

- [x] 9.1 Run `./gradlew test` and `./gradlew lint` for `:app`, `:wear` and `:shared` from `src/`; fix failures and report results verbatim — 290 unit tests across the three modules, 0 failures. Lint BUILD SUCCESSFUL: `:app` has only its 9 known `PluralsCandidate` warnings, `:wear` is clean after adding the same `bundle { language { enableSplit = false } }` the phone uses
- [x] 9.2 Run `./gradlew :wear:connectedAndroidTest` on a Wear emulator and `./gradlew :app:connectedAndroidTest` for the unchanged phone suites — **not on a Wear emulator**: this machine has no Wear OS system image and no `cmdline-tools`, so none can be installed. The watch tests ran on the API 36 phone emulator, where the 7 screen tests pass (they are semantics tests and do not depend on the device's shape) and the 2 Data Layer tests skip for want of the Wearable API. Round and square rendering is covered by previews and by manual cases 9 and 10. The phone suite: 172 tests, 0 failures
- [x] 9.3 Update `README.md`: technology table gains "Wearable: Wear OS companion app with Compose for Wear OS" and "Phone to watch sync: Wearable Data Layer (Play services)"; repository layout lists the `wear` and `shared` modules; "Features" gains the watch list; a privacy note states what is synced to the watch and that it stays between the two devices — and the build section now says both applications must be signed with one certificate
- [x] 9.4 Review against `CLAUDE.md`: no network permission, dependency justified and disclosed, strings in resources with translations, no sensitive logging, single DI mechanism per app, version catalog, glossary terms
