## Context

The phone app (`:app`) owns all data: medications, schedules and, after `app-medicine-alarm`, dose records in Room, with a `ReminderCoordinator` that already reacts to every dose change. A paired Wear OS watch receives the reminder notification through platform bridging and can answer it. There is no code on the watch.

The `app-medicine-alarm` design rejected a Wear OS companion app because the bridge covered the reminder moment. This change adds one for a different reason: a glanceable list of what is coming up, which a notification cannot provide. It must not disturb what already works (bridged notifications) and must not turn the watch into a second source of truth.

`app-settings-language` makes the phone app render in a user-chosen language (English or Dutch) independent of the system, including notifications built in receivers, through a localised context. The watch must follow the same language or the user sees a Dutch phone and an English watch.

Constraints from `CLAUDE.md`: Kotlin, Compose, MVVM, no network permission, no third-party SDKs without a named reason and README disclosure, strings in resources, accessibility, no logging of names or doses, dependencies in the single version catalog created by `app-welcome-screen`, small files, brand colours and intake state colours from `docs/design-system.md` on the watch as well (the phone's type scale and spacing tokens do not transfer to a round watch screen; Wear Material 3 supplies those).

## Goals / Non-Goals

**Goals:**
- A Wear OS app with one read-only screen listing pending doses in the next six hours with name, amount and time, and the exact empty-state text requested.
- A phone-to-watch sync that is simple, resilient to disconnection, and carries no more than the watch needs.
- The watch renders in the phone app's language.
- Bridged reminder notifications keep working unchanged.
- A shared contract module so the two apps cannot drift on the wire format.

**Non-Goals:**
- Any write path from the watch. No confirm, snooze, skip, add or edit.
- Standalone operation, tiles, complications, ongoing activities.
- Moving the domain layer into a shared module. The watch does not need the domain model; it needs a display list.

## Decisions

### D1. Project structure: `:wear` application module and `:shared` contract module

```
src/
  app/        phone application (unchanged layout)
  wear/       Wear OS application, applicationId nl.hexmaster.pillsner, minSdk 30
  shared/     plain Kotlin (JVM) module: sync contract only
  gradle/libs.versions.toml
```

`:shared` contains only `WearSyncContract` (data path, keys), `SyncedDose`, `SyncedDoses` and their `kotlinx-serialization` JSON. It has no Android dependency, so both apps and their unit tests use it directly.

*Why not a shared domain module:* the watch shows a list, it does not reason about schedules, doses or intakes. Pulling `Medication`, `Schedule`, `Quantity` and the formatter into a shared module is the multi-module refactor the welcome-screen design deferred, and `CLAUDE.md` says refactors get their own proposal. A contract module gives the type safety that matters (both sides compile against one payload) without the move.

*Why the same application id:* the Wearable Data Layer only connects a phone app and a watch app with the same package name and signing certificate; Play also uses it to offer the watch app when the phone app is installed. `signingConfigs` are shared through the root build so debug builds pair too.

### D2. What the phone publishes

```kotlin
@Serializable data class SyncedDose(
    val doseId: Long,
    val medicationName: String,
    val amountText: String,        // already formatted by the phone, e.g. "40 mg", "2 tabletten"
    val scheduledAtEpochMillis: Long,
)
@Serializable data class SyncedDoses(
    val version: Int = 1,
    val languageTag: String,       // BCP 47, the phone app's effective language, e.g. "nl-NL"
    val publishedAtEpochMillis: Long,
    val doses: List<SyncedDose>,   // all pending doses in the phone's two-day window, ordered by time
)
object WearSyncContract { const val PATH = "/pillsner/upcoming-doses"; const val KEY_JSON = "json" }
```

The phone publishes **every pending dose it has** (the rolling two-day window from `app-medicine-alarm`, typically under twenty rows), not just the next six hours. The watch does the six-hour filtering with its own clock (D5). This way the watch keeps showing the right list for up to two days when the phone is unreachable, and the phone never needs a timer to re-publish as the window slides.

`amountText` is pre-formatted on the phone with the existing `QuantityFormatter` under the app's localised context. *Why a display string on the wire:* it keeps `Quantity`, `DoseUnit` and the plural resources out of the watch entirely, and the amount is a snapshot anyway. The phone is the only place that knows the units and their translations. The trade-off (the watch cannot re-format) does not matter because the watch renders in the same language, carried in `languageTag`.

`scheduledAtEpochMillis` is an instant; the watch formats it in its own zone and the payload language, so a time-zone difference between the devices cannot arise (they are paired and share a zone in practice, and the instant is right either way).

`version` lets a newer phone talk to an older watch: the watch ignores unknown fields (`ignoreUnknownKeys = true`) and refuses payloads with a higher major version by showing the disconnected footer.

### D3. Transport: one `DataItem`, replaced on every change

`DoseSyncPublisher(dataClient, doseRepository, quantityFormatter, localeProvider, clock)` in the phone's data layer collects `doseRepository.observePending()`, maps to `SyncedDoses`, and calls `dataClient.putDataItem(PutDataMapRequest.create(PATH).apply { dataMap.putString(KEY_JSON, json) }.asPutDataRequest().setUrgent())`. `publishedAtEpochMillis` changes every time, so the Data Layer sees a changed item and syncs it even when the dose list is identical, which is what we want after a language change.

The publisher runs inside the existing `ReminderCoordinator` scope (application-scoped, `Dispatchers.IO`), starts at app start and after every `onWake`, and is debounced by 500 ms so a burst of dose writes produces one publish. When Play services is unavailable on the phone (`GoogleApiAvailability` not `SUCCESS`) the publisher is a no-op and logs at debug level; the phone app is otherwise unaffected.

*Why `DataClient` rather than `MessageClient`:* data items are persisted by Play services on both devices and delivered when the watch next connects, so the watch always has the latest list on open without the phone being reachable at that moment. Messages are fire-and-forget to connected nodes and would need our own retry and storage.

*Why one item at a fixed path rather than one per dose:* the list is small, one item makes "replace the whole list" atomic and keeps deletion trivial (a dose that was taken simply is not in the next payload).

*Privacy:* the Data Layer moves items over the Bluetooth or local Wi-Fi link between paired devices; Wear OS removed the cloud relay for data items. Neither module declares `INTERNET`. Still, medicine names now exist on a second device, in Play services storage, which is why the README says so. The task list includes verifying the no-cloud property against the current Play services documentation before archiving.

**Verified on 13 September 2026** against the current Wear OS documentation:

- *Sync data items with the Data Layer API* (`developer.android.com/training/wearables/data/data-items`) states that "the Data Layer API can only send messages and synchronize data with Android phones or Wear OS watches", and directs apps that want to reach a network to *Communicate directly over a network* instead. The Data Layer is therefore not a path to a server.
- *Sync persistent data* (`developer.android.com/training/wearables/data/sync`) describes the transport as the Bluetooth link between the paired devices, and says assets are for sharing "large binary objects over the Bluetooth transport".

Neither page describes a cloud relay for data items, and the merged manifests of both modules were checked after adding the dependency: `:app` declares only the four permissions it already had, `:wear` declares none. Play services Wearable adds no `INTERNET` or `ACCESS_NETWORK_STATE` permission of its own.

### D4. Watch side: reading, listening, waking

`UpcomingDosesRepository` (watch, data layer) exposes `Flow<SyncedDoses?>`:
- initial value from `dataClient.getDataItems(uri)` for our path (persisted item, available offline),
- updates from `dataClient.addListener` while the flow is collected,
- `null` when there is no item yet.

`WearDataListenerService : WearableListenerService` is declared in the manifest with a `DATA_CHANGED` filter for the path so Play services can start the app process when new data arrives while the app is closed. It does nothing but let the persisted item update; the screen reads the persisted item on open. Keeping it empty avoids background work on the watch.

`PhoneConnectivity` wraps `NodeClient.connectedNodes`, refreshed on screen resume and every minute with the ticker, yielding `Boolean`.

### D5. Watch UI state and the six-hour window

```kotlin
data class WatchUiState(
    val entries: List<WatchDoseEntry>,   // filtered, ordered
    val isEmpty: Boolean,                // no entries after filtering
    val phoneConnected: Boolean,
    val hasData: Boolean,                // false until the first payload ever arrived
    val locale: Locale,
)
data class WatchDoseEntry(val doseId: Long, val name: String, val amountText: String, val scheduledAt: Instant, val isTomorrow: Boolean)
```

`UpcomingWindowFilter` (pure Kotlin, in `:wear` but Android-free) keeps doses with `scheduledAt <= now + 6h` and drops nothing on the past side. The phone never publishes lapsed (missed) doses, so anything in the payload with a past time is pending and unanswered, and it belongs at the top. Ordered by `scheduledAt` ascending.

`WatchViewModel` combines the repository flow, the connectivity flow and a minute ticker (a `flow { while(true) { emit(now); delay(untilNextMinute) } }`) into `WatchUiState`. The ticker makes doses enter the window and the empty state appear or disappear without any data change.

*Why include overdue pending doses:* the request says "doses scheduled for the upcoming 6 hours", and a dose due twenty minutes ago that the user has not answered is still a dose to take now; hiding it on the watch while the phone Home shows it would be inconsistent and unsafe. Flagged in the proposal.

### D6. Screen layout (Compose for Wear OS, Material 3)

`AppScaffold` with `TimeText` at the top, `ScreenScaffold` with a `TransformingLazyColumn`:
1. `ListHeader` "Next 6 hours".
2. One `TitleCard`-style card per entry: title = medicine name, subtitle = amount, time as the trailing label (`HH:mm` in the payload locale, prefixed with "Tomorrow" when the date differs from today; within six hours that is the only possible other day). The whole card is one semantics node reading "Ibuprofen, 40 mg, at 14:00".
3. Empty state: a centred `Text` "No medicines scheduled for the upcoming 6 hours" replacing the list, with the header kept.
4. Footer (last list item) when `phoneConnected == false`: "Phone not connected" in a de-emphasised style; when `hasData == false` and not connected: "Open Pillsner on your phone to sync" instead.

The layout follows Wear guidelines: padding for round screens from the scaffold, `TransformingLazyColumn` scaling and fading, rotary input supported by the column, minimum 48 dp card height, text at the Wear typography scale so the system font size setting applies.

**Theme on the watch.** The `:wear` module has its own small `ui/theme` with a Wear Material 3 `ColorScheme` built from the design system's dark column (section 2.2: watch surfaces are always dark, so `primary` `#8CD8B0`, `secondary` `#A2C9FF`, `tertiary` `#83D4E0`, `error` `#FFB4AB`, `surface` `#101413` and their containers), the only file in that module with hex literals. Overdue pending doses use `errorContainer` / `onErrorContainer` with the `error` icon, due doses `secondaryContainer` with `schedule`, exactly as section 2.3 maps them, so the wrist shows the same states as the phone. Typography and shapes stay the Wear Material 3 defaults: the design system's Montserrat and Raleway scale is designed for phone screens, and bundling another megabyte of fonts on a watch buys nothing. Icons are Material Symbols Rounded bundled as vector drawables.

*Alternative considered:* `ScalingLazyColumn` from Wear Compose foundation. The Material 3 `TransformingLazyColumn` is the current recommendation and comes with the M3 components; both are first-party.

### D7. Language on the watch

The watch's string resources ship in `values/` (English) and `values-nl/`, with the same completeness lint rule `app-settings-language` introduces (`MissingTranslation` as error). At render time the watch wraps content in `CompositionLocalProvider(LocalConfiguration provides configWith(languageTag))` derived from the payload, and uses a `createConfigurationContext` for `stringResource` and time formatting. When no payload exists yet, the watch system locale is used.

*Why follow the phone app's language rather than the watch system language:* the amounts are already formatted in the phone's language; mixing "2 tabletten" with an English header would look broken, and the user chose that language deliberately in the phone app.

### D8. Notification bridging stays on

The watch manifest does **not** set `com.google.android.wearable.notificationBridgeMode` to `NO_BRIDGING`, so phone reminder notifications keep appearing on the watch with their actions. The spec makes this a requirement and the manual test checks it, because it is the single most likely regression from adding a watch app.

### D9. Dependencies and build

Version catalog additions, at the newest stable versions on 11 September 2026 and re-checked at apply time: `com.google.android.gms:play-services-wearable` 20.0.1 (April 2026; carries a fix recommended for apps targeting API 37), `androidx.wear.compose:compose-material3` and `compose-foundation` 1.6.2 (May 2026; 1.7.0 is still a release candidate), `androidx.wear.compose:compose-ui-tooling` 1.6.2, `androidx.wear:wear-tooling-preview` 1.0.0; `kotlinx-serialization-json` is already in the catalog at 1.11.0 from `app-welcome-screen`. The `:wear` module uses the same Compose BOM (2026.08.00), Kotlin (2.4.20) and Kotlin Compose plugin as `:app`, with `compileSdk` and `targetSdk` 37 and `minSdk` 30. `:shared` applies `kotlin("jvm")` with the JDK 21 toolchain and the serialization plugin. `:wear` manifest: `<uses-feature android:name="android.hardware.type.watch" />`, `<meta-data android:name="com.google.android.wearable.standalone" android:value="false" />`, the listener service, no permissions except none (the Data Layer needs none).

*Why Play services Wearable is justified:* it is the only API for phone-to-watch communication on Wear OS; there is no AndroidX equivalent. It is Google's SDK, not a third-party analytics or network SDK, and it is used solely for the paired-device link.

### D10. Logging

Both publisher and watch repository log at debug level only with dose counts and ids. No names, no amounts.

### D11. Package layout

```
shared/src/main/kotlin/nl/hexmaster/pillsner/shared/wear/WearSyncContract.kt, SyncedDoses.kt
app/.../data/wear/DoseSyncPublisher.kt, WearDataClientFactory.kt
wear/src/main/kotlin/nl/hexmaster/pillsner/wear/
  WearApp.kt, MainActivity.kt, WearContainer.kt
  data/UpcomingDosesRepository.kt, WearDataListenerService.kt, PhoneConnectivity.kt
  domain/UpcomingWindowFilter.kt
  ui/WatchViewModel.kt, WatchUiState.kt, UpcomingDosesScreen.kt, DoseCard.kt, LocalizedContent.kt, theme/
wear/src/main/res/values/strings.xml, values-nl/strings.xml
```

## Risks / Trade-offs

- [Reversing the "no Wear app" decision from `app-medicine-alarm`] → The reason has changed: a glanceable list, not reminder delivery. Bridged notifications remain the reminder path; the watch app adds only a read-only view.
- [Stale data on the watch when the phone is unreachable] → The watch filters the full two-day payload with its own clock and shows a "Phone not connected" footer. A dose answered on the phone while disconnected still shows on the watch until reconnection; the footer tells the user why.
- [Play services missing or outdated on the phone or watch] → Publisher becomes a no-op and the watch shows the "open Pillsner on your phone to sync" footer. Wear OS devices always ship Play services; the phone-side check covers de-Googled phones.
- [Data Layer transport assumptions (no cloud relay)] → Verified against current documentation as a task before archiving, and disclosed in the README either way.
- [Same application id across two modules complicates Play publishing] → It is the required setup for Wear companion apps and Play supports multi-APK/AAB per form factor; documented in the README build section.
- [Watch and phone clocks differ] → Instants on the wire; the six-hour window is computed on the watch clock. A few seconds' skew is invisible at minute granularity.
- [Payload version drift after future changes] → `version` field, `ignoreUnknownKeys`, and the rule that additive changes bump nothing while removals bump the major version and are handled by showing the sync footer.
- [Compose for Wear OS Material 3 API churn] → Pinned in the version catalog; the screen is a single file so an upgrade is a small change.

## Migration Plan

Additive. Existing phone installs gain the publisher; a watch without the app receives data items it ignores (Play services drops items for uninstalled packages). Installing the watch app later picks up the last published item. Rollback is removing the two modules and the publisher; persisted data items on the watch are deleted with the app.

## Open Questions

- Should the watch also offer a Tile or complication showing the next dose? Recommended next change once this screen has been used.
- Should the six-hour window be a setting? Not until asked.
- Should the watch app hide medicine names behind a tap for privacy at a glance, like the notification's public version? Recommended: no by default; a wrist is more private than a lock screen, and the setting question belongs with the lock-screen one in `app-medicine-alarm`.
- When a language change happens on the phone, the watch updates on the next publish, which happens immediately; but if the phone is disconnected the watch keeps the old language until reconnection. Acceptable.
