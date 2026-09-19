# Optimization backlog

Findings from a scan of the `app` and `wear` Gradle modules (plus `shared`) on 2026-09-16.
These are optimization opportunities, not bugs — behaviour is correct today. Each item should
go through the normal OpenSpec workflow if it changes behaviour or touches scheduling logic;
pure internal refactors with no behaviour change can be done directly per CLAUDE.md's rule of
thumb for small, clearly-scoped fixes.

## Reminders and scheduling (`data/reminders`, `domain/scheduling`)

- [ ] `domain/scheduling/MarkMissedDoses.kt:35-52`, `domain/scheduling/DueDoses.kt:23-35`,
  `domain/scheduling/ComputeWakeSchedule.kt:40-61` — every wake cycle calls
  `doseRepository.pending()` three separate times (once per use case), and
  `MarkMissedDoses.lapseAt(dose)` issues its own `nextScheduledAtAfter(...)` query per pending
  dose. Up to 3×(1 + N) redundant DB round-trips per wake. Compute pending doses and each dose's
  lapse moment once, then pass the results down.
- [ ] `domain/scheduling/RefreshPlannedDoses.kt:61` and `domain/scheduling/ComputeWakeSchedule.kt:75`
  — both independently collect `medicationRepository.observeAll().first()` in the same wake
  cycle. `RefreshPlannedDoses` already has the list; reuse it instead of re-reading.
- [ ] `data/RoomDoseRepository.kt:41-51` (`refreshSnapshots`) — loops over every planned dose and
  calls `dao.refreshSnapshot(...)` once per dose (separate UPDATE/commit per row), not wrapped in
  `@Transaction`. Runs on every wake for every dose in the rolling window; batch into one
  transaction.
- [ ] `data/RoomDoseRepository.kt:63-73` (`withdrawPlanned`) — loops over medications issuing one
  `dao.plannedNoLongerScheduled(...)` query per medication instead of a single batched query.
- [ ] `data/reminders/ReminderCoordinator.kt:190-209` (`wake()`) — the `due.forEach` loop calls
  `recordReminded(...)` / `setSnooze(...)` as separate suspend DAO calls per dose instead of one
  transaction. Same pattern as `refreshSnapshots`; worth fixing together.
- [ ] `data/reminders/ReminderDeliveryLog.kt:65-81` (`append()`) — calls `trimIfNeeded()`
  unconditionally, which does `file.readLines()` (a full read of the whole log file) on every
  single `record()` call, and `record()` fires multiple times per wake. Track the line count in
  memory, or only read the file when its on-disk size suggests it's near the trim threshold.

## Room / data layer (`data/db`, repositories, DataStore, DI)

- [ ] `data/db/DoseDao.kt:15,28` (`observePending()` / `pending()`) — filters
  `WHERE outcome IS NULL ORDER BY scheduled_at ASC`, but `DoseEntity`'s indices
  (`data/db/DoseEntity.kt:28-31`) only cover `(medication_id, scheduled_at)` and
  `(scheduled_at)`, not `outcome`. Doses are never deleted, so this table grows unbounded and
  these frequently-collected Flows scan increasingly more rows. Add a composite index on
  `(outcome, scheduled_at)`.
- [ ] `di/AppContainer.kt:171-178` — the `theme` `StateFlow` calls
  `themeRepository.observeTheme()` twice: once via `runBlocking { .first() }` for the initial
  value, again as the upstream of `stateIn`. Two independent collectors against the same
  DataStore Preferences flow at startup. Derive both the blocking initial value and the hot flow
  from a single subscription.

## App-lock, legal, reset domain

- [ ] `applock/data/KeystorePinVerifier.kt:35-37,48-49` — `verify()` calls `isAvailable()`
  (`containsAlias` + `getKey`) and then `hmac()` → `loadOrCreateKey()`, which calls
  `keyStore.getKey(...)` again. Every PIN check does two separate `AndroidKeyStore.getKey`
  round-trips instead of reusing the key already fetched.
- [ ] `applock/domain/UnlockWithPin.kt:28`, `VerifyIdentity.kt:112`, `ChangePin.kt:30`,
  `IsCurrentPin.kt:15`, `EnablePinLock.kt:12` — all call the blocking `PinVerifier` (KeyStore +
  `Mac` init, JCE crypto) directly inside suspend functions with no
  `withContext(Dispatchers.IO)`. Since these run under `viewModelScope` (Main), keystore/HMAC
  work risks running on the main thread on every unlock attempt, PIN change, and biometric
  fallback.
- [ ] `PillsnerApplication.kt:78` (`applyStoredLanguage()`) — uses
  `runBlocking { languageRepository.observeLanguage().first() }`, called from `onCreate()` and
  from `onConfigurationChanged()`. The latter always runs on the main thread, so every system
  locale change blocks the UI thread on a DataStore/disk read.
- [ ] `domain/model/Quantity.kt:35,43` — `equals()` and `hashCode()` each independently call the
  `normalizedValue` getter, which allocates via `value.stripTrailingZeros()` on every call rather
  than caching it once. `Quantity` is used pervasively in collections (`distinct()`, `Set`
  membership).

## Compose UI (`ui/**`, `applock/ui/**`)

- [ ] `ui/medicines/MedicinesScreen.kt:196,230` — schedule descriptions are computed inline in the
  `items(...)` lambda on every recomposition. Because a single shared `revealedId` state is read
  by every tile's `isRevealed` check, opening/closing the swipe action on *any* tile recomposes
  every visible tile and re-runs `ScheduleDescriptionFormatter.describe` for unrelated tiles.
  Wrap in `remember(tile.schedules) { ... }`.
- [ ] `ui/medicines/form/MedicationFormViewModel.kt:242-252` (`updateDraft`) — calls
  `DraftSaver.saveInitial(savedStateHandle, initialDraft)` unconditionally on every draft
  mutation (every keystroke), even though `initialDraft` only changes inside `load()`. Re-encodes
  every schedule via `ScheduleCodec.encode` and rewrites 9 `SavedStateHandle` entries per
  keystroke for no reason.
- [ ] `ui/settings/diagnostics/ReminderDiagnosticsScreen.kt:159` — `itemsIndexed(entries, key =
  { index, _ -> index })` uses list index as the `LazyColumn` key. Since `entries` is
  newest-first and grows at the front, every row's key shifts on each new entry, forcing every
  visible row to recompose instead of just the new one. Use `entry.at.toEpochMilli()` (or similar)
  as a stable key instead.

## Wear module (`wear/**`) and shared

- [ ] `wear/src/main/kotlin/.../wear/ui/WatchViewModel.kt:35-56` — the cold `minuteTicker` flow is
  passed into `combine()` twice (directly, and again via `connectivity =
  minuteTicker.map { isPhoneConnected() }`). Since it isn't shared (`shareIn`/`stateIn`), two
  unsynchronized minute-tick coroutines run concurrently, doubling the ticking and causing
  `UpcomingWindowFilter.filter` + list mapping to run twice per minute. Call `isPhoneConnected()`
  directly inside the `combine` transform instead of via a second derived flow.
- [ ] `wear/src/main/kotlin/.../wear/ui/DoseCard.kt:99-103` (`formatTime()`) — builds a new
  `DateTimeFormatter` on every call, invoked from `timeText()` on every recomposition of every
  visible `DoseCard`. Wrap in `remember(locale, zone)`.
- [ ] `wear/src/main/kotlin/.../wear/domain/UpcomingWindowFilter.kt:23` — allocates a new
  `Instant.ofEpochMilli(...)` per dose on every filter call (driven by the minute ticker above,
  so effectively per minute per dose, doubled by the ticker issue). Compare raw epoch millis
  longs instead.

## Reviewed, no issues found

For completeness — these areas were read in full and found efficient for their scale, so they
don't need follow-up: `ArmedAlarmStore`, `ReminderAlarmScheduler`, `ReminderActionReceiver` /
`ReminderAlarmReceiver` (correct `goAsync()` + coroutine handoff), `ReminderWakeService`,
`ReminderWatchdog`, `ReminderNotifier`, `ReminderChannels`, `ReminderPreferences`,
`SystemEventsReceiver`, `UserUnlockState`, `BatteryOptimisationState`, `DoseGenerator`,
`ReminderRepeats`, `WakeSchedule`, `SilentlyMissedReminder`, `AnswerDose`, `DoseTiming`,
`RecordIntake`, `SnoozeDose`, `DoseSyncPublisher`, `WearDataClientFactory`; `Pin.VALID` regex
caching, `SummariseUsageHistory` bucket scan (bounded to ~13 buckets), `LocaleResolver`,
`IsLegalAccepted`, `ScheduleDraftValidator` / `MedicationFormValidator`,
`LockOnBackgroundObserver`; all `DateTimeFormatter` / `NumberFormat` / `Collator` /
`DecimalFormat` usage elsewhere in `ui/` and `applock/ui/` (already `remember`-cached); wear's
`PhoneConnectivity`, `UpcomingDosesRepository`, `WearDataListenerService` (URI-filtered listener
registration), `LocalizedContent`, `UpcomingDosesScreen`, `WatchUiState`, `WearDimens`,
`WearTheme`, `SyncedDoses`, `WearSyncContract`, `MainActivity`, `WearContainer`,
`PillsnerWearApplication`; release build config already has `isMinifyEnabled` /
`isShrinkResources` set for both `app` and `wear` with no duplicated version declarations outside
the catalog.
