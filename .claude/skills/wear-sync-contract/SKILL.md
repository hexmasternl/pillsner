---
name: wear-sync-contract
description: Change what Pillsner's phone app sends to the Wear OS app without breaking a watch or phone on a different version - the SyncedDoses payload and WearSyncContract path in src/shared, the phone's DoseSyncPublisher and the watch's UpcomingDosesRepository - with the rules for additive versus breaking changes, when to bump CURRENT_VERSION, and the compatibility tests to write. Use whenever a change touches src/shared, the phone's wear publisher, or what the watch displays.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

The phone and the watch apps are released together, but they update independently on people's
devices: for days a new phone app can talk to an old watch app, or the other way round. The payload
between them is therefore a contract, and every change to it must work across versions.

## The pieces

| Piece | File |
| --- | --- |
| The Data Layer path and key | `src/shared/src/main/kotlin/nl/hexmaster/pillsner/shared/wear/WearSyncContract.kt` (`PATH = "/pillsner/upcoming-doses"`, `KEY_JSON = "json"`) |
| The payload | `.../shared/wear/SyncedDoses.kt`: `SyncedDoses` (with `version`, `CURRENT_VERSION = 1`), `SyncedDose`, `SyncedMedicineDetails`; decoding with `ignoreUnknownKeys = true`, returning null for malformed or later-version payloads |
| Phone side (publisher) | `src/app/src/main/java/nl/hexmaster/pillsner/data/wear/DoseSyncPublisher.kt` |
| Watch side (reader) | `src/wear/src/main/kotlin/nl/hexmaster/pillsner/wear/data/UpcomingDosesRepository.kt`, shown by `wear/ui/WatchViewModel.kt` |
| Tests | `src/shared/src/test/.../SyncedDosesTest.kt`, `wear/src/androidTest/.../UpcomingDosesRepositoryTest.kt` |

## Is the change additive or breaking?

**Additive** — no version bump:
- A new field **with a default value** (`val x: String? = null`, `= emptyList()`). An old watch
  ignores it (`ignoreUnknownKeys`); a new watch reading an old phone's payload gets the default.
- The new watch must behave sensibly when the field is absent (the default) — that's the case of an
  old phone.

**Breaking** — bump `CURRENT_VERSION`:
- Removing or renaming a field, changing its type, or changing what an existing field means (e.g.
  `amountText` becoming a number, or `scheduledAtEpochMillis` becoming local time).
- A watch that meets a higher version shows its "update the app" state instead of misreading data;
  a phone keeps publishing what the watches it might meet can read. Prefer an additive design
  whenever one exists — a breaking bump means watches show nothing useful until they update.

Changing `PATH` or `KEY_JSON` is always breaking and needs a design decision for the transition
(publish both for a release).

## Checklist

1. Decide additive vs breaking in the change's design, and say why.
2. Edit the payload in `shared`, with KDoc on any new field saying what it holds and its unit/zone
   (times are epoch millis; text is already localised on the phone, in the phone's language, with
   `languageTag` saying which).
3. Update the publisher and the reader in the same change.
4. **Tests** in `SyncedDosesTest` (unit, fast):
   - Round trip of the new payload.
   - **Old phone → new watch**: decode a JSON fixture of the previous payload shape (copy the current
     encoded output into the test as a string literal *before* changing the classes) and check the
     defaults.
   - **New phone → old watch**: the new JSON still decodes with the old field set — for additive
     changes, guaranteed by `ignoreUnknownKeys`; assert it once anyway.
   - For a breaking change: a payload with `version = CURRENT_VERSION + 1` decodes to null.
5. Run `:shared:test`, `:wear:test`, `:app:test`, and the `wear` instrumented tests (the
   `emulator-tests` skill selects all of `wear` for a `shared` change).
6. Keep the payload small. The Data Layer limits a data item to 100 KB, and it's replaced whole on
   every change; the list holds only the upcoming doses the watch shows.

## Guardrails

- Never send more user data to the watch than it displays. Medicine names already travel to the
  watch; anything new that does (notes, stock, prescriber) is a privacy decision to name in the
  proposal and the website's privacy page.
- Never change the meaning of an existing field in place. Add a new field, and remove the old one
  later with a version bump if it must go.
- Never rely on both apps updating at the same moment.
