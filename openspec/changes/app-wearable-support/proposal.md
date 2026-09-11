## Why

With `app-medicine-alarm` a paired Wear OS watch already shows the reminder notification and its three answers, but only at the moment a dose is due. A glance at the wrist cannot answer "what do I have to take in the next few hours?" without pulling out the phone. A small wearable app that shows exactly that, and nothing else, completes the reminder loop for the people who wear a watch.

## What Changes

- **A Wear OS app**, delivered as a second application module in the same Gradle project, with the same application id and signing as the phone app so the two can talk through the Wearable Data Layer. It is a companion, not a standalone app: it requires the phone app.
- **One screen.** The watch app shows the list of doses scheduled for the **upcoming six hours**, ordered by time, each entry with the medicine name, the dose amount and the scheduled time. Pending doses whose time has already passed but that have not been answered are included at the top, because they are still to be taken. Doses that are taken, skipped or missed are not shown.
- **Empty state.** When nothing is scheduled in that window, the screen shows "No medicines scheduled for the upcoming 6 hours".
- **Read-only.** The watch app has no add, edit, confirm, snooze or skip actions. Answering a dose stays where it is today: the bridged reminder notification on the watch, or the phone.
- **Sync from phone to watch.** The phone publishes its pending doses for the current two-day window to the watch whenever they change, together with the phone app's language so the watch shows text in the same language. The watch keeps the last received data and filters it to the six-hour window with its own clock, refreshing every minute, so the list stays correct while the phone is out of reach. When no phone is connected the screen says so in a footer, so a stale list is never mistaken for a live one.
- **Notifications keep bridging.** Installing the watch app must not stop the phone's reminder notifications from appearing on the watch with their actions.
- **New dependency: Google Play services Wearable** (`play-services-wearable`) on both the phone and the watch. It is the only supported transport between a phone app and a Wear OS app. It moves data over the direct Bluetooth or local network link between the paired devices; no Pillsner data goes to a server, and the app still declares no network permission. The README discloses the dependency.
- **Compose for Wear OS** (Material 3 for Wear) for the watch UI, matching the phone's Compose and Material 3 choice.
- **Language.** The watch's strings get the same English and Dutch translations as the phone, and the watch renders in the language the phone app is set to.

## Capabilities

### New Capabilities
- `wearable-app`: The Wear OS application: its single screen, the six-hour list, entry content, ordering, the empty state, the phone-disconnected footer, minute-by-minute refresh, language, accessibility on round and square screens, and the rule that it performs no maintenance actions.
- `wearable-sync`: The phone-to-watch data contract: what is published, when, in which format, how the watch stores and reads it, the transport and its privacy properties, and the rule that both apps share application id and signing.

### Modified Capabilities
- `medicine-reminders`: The "Wearable delivery" requirement gains the rule that the watch app does not disable notification bridging, and clarifies that dose answers on the wearable come from the bridged notification, not the watch app. This spec is a delta inside the active `app-medicine-alarm` change; that change MUST be archived before this one.

## Impact

- **Gradle project (`src/`)**: two new modules. `:wear` is the Wear OS application (Compose for Wear OS, Material 3 for Wear, `minSdk` 30). `:shared` is a plain Kotlin module holding the sync contract: the data path, the `SyncedDose` and `SyncedDoses` payload types, and their JSON serialisation with `kotlinx-serialization`, already a project dependency. The version catalog gains Wear Compose, Play services Wearable and `kotlinx-serialization-json`.
- **Phone app**: a `DoseSyncPublisher` in the data layer observes pending doses, formats each amount with the existing `QuantityFormatter` in the app's language, and writes one data item through `DataClient`. It runs from the existing `ReminderCoordinator` scope on every dose change and on app start. `AppContainer` wires it.
- **Watch app**: a single activity with an `UpcomingDosesScreen`, a view model that reads the persisted data item, listens for changes and ticks every minute, a connectivity check through `NodeClient`, and a `WearableListenerService` so the app is woken when data arrives while it is not running.
- **Dependencies**: `com.google.android.gms:play-services-wearable` (phone and watch), `androidx.wear.compose:compose-material3`, `androidx.wear.compose:compose-foundation`, `androidx.wear:wear-tooling-preview`, `org.jetbrains.kotlinx:kotlinx-serialization-json`. No network permission on either module.
- **Depends on**: `app-medicine-alarm` (dose records, `DoseRepository`, `ReminderCoordinator`) and `app-settings-language` (the localised context and the supported-language list) being applied first.
- **Privacy**: medicine names and amounts travel to the watch over the paired-device link and are stored by Play services on the watch. Nothing is logged at info level or above on either side. The README explains this.
- **Tests**: unit tests for the six-hour filter, ordering and the empty state at window boundaries; serialisation round trip; publisher tests with a fake data client; Compose tests on a Wear emulator for the list, the empty state and the footer; manual test cases for pairing, phone out of range, language switch and notification bridging.
- **README**: technology table gains Wear OS rows; "Features" gains the watch app; the dependency and privacy notes are added.

## Non-goals

- Any action on the watch: confirming, snoozing or skipping a dose, adding or editing medicines. Answers stay in the bridged notification.
- A standalone watch app that works without the phone app.
- Wear OS tiles, complications, or ongoing activities. Natural follow-ups, proposed separately.
- Support for wearables that are not Wear OS.
- Showing doses further than six hours ahead, or history.
- Changing the phone app's reminder notification.
