## Why

`reminder-delivery-reliability` added a system battery-optimisation dialog that opens over the Home
screen the first time a user has an upcoming dose. It interrupts the user with a platform prompt
about battery before they have asked for anything, and it buys Pillsner almost nothing: the
exemption's only documented effects are network access and partial wake locks during Doze, and
Pillsner has no network and holds no wake lock of its own. Everything the reminder path actually
depends on is already granted by permissions the app holds.

The prompt therefore costs a jarring interruption, a permanently-shown Home banner, a
Play-policy-sensitive permission and a standing instrumented-test hazard, in exchange for a benefit
the same change's own D1 already delivers by other means.

## What Changes

- **BREAKING (user-visible):** the app no longer opens the battery-optimisation dialog by itself.
  Nothing is requested unprompted; the exemption becomes something the user can reach, not something
  the app asks for.
- **BREAKING (manifest):** `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is removed. Without it the one-tap
  `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` dialog is unavailable, so the remaining route is
  `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` — the system list — which needs no permission. The
  `@SuppressLint("BatteryLife")` suppression and the Play declaration burden go with it.
- The battery banner stops being raised by the mere absence of the exemption. It is raised by
  evidence instead: a dose that lapsed with no reminder ever posted, which is the observable symptom
  of an alarm the platform did not deliver. Until that happens, a non-exempt device is treated as
  healthy, because on the alarm-clock tier it is.
- The banner's wording changes from a claim about battery optimisation to a statement of what was
  observed ("a reminder did not arrive"), with the same route to the system list and the same
  vendor auto-start offer on OEMs that ship one.
- Evidence of a missed reminder is recorded and cleared: it is set when a dose lapses un-reminded and
  cleared when the user acts on the banner or when a reminder posts successfully afterwards.
- The banner precedence order keeps three entries but the battery entry now means "reminders have
  actually been missed" rather than "the exemption is absent".
- `ReminderPreferences.hasRequestedBatteryExemption` becomes dead and is removed, along with
  `HomeViewModel.shouldRequestBatteryExemption` and `BatteryOptimisationEffect`'s request branch.
- The instrumented-test precondition added by `reminder-delivery-reliability` task 9.3 —
  whitelisting the app before a run so the dialog does not cover the UI — is no longer needed and is
  removed from the manual test notes.

Not changing: `setAlarmClock` for reminders, `USE_EXACT_ALARM`, the watchdog, the wake service, the
per-dose alarm set, or anything about how a reminder is scheduled, posted or answered. This change
removes an intervention; it does not touch the delivery path.

## Capabilities

### New Capabilities

None. This change modifies behaviour introduced by `reminder-delivery-reliability`.

### Modified Capabilities

- `reminder-delivery-resilience`: the "Battery optimisation exemption is requested" requirement is
  removed outright; "Home reports when the system is throttling reminders" is rewritten so the
  banner is driven by an observed missed reminder rather than by the exemption state; "Vendor
  auto-start guidance on known devices" keeps its behaviour but hangs off the new trigger.

**Prerequisite:** `reminder-delivery-reliability` must be archived before this change can be applied,
because `reminder-delivery-resilience` does not yet exist in `openspec/specs/`. Two tasks remain open
there (9.3 and 9.3a, both instrumented-test runs). This change's delta is written against that
change's delta spec and will apply cleanly once it lands.

## Impact

**Code**

- `src/app/src/main/AndroidManifest.xml` — remove the `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
  permission and its comment. The `<queries>` block stays; the vendor screens are still offered.
- `src/app/src/main/java/nl/hexmaster/pillsner/ui/home/BatteryOptimisationRequest.kt` — drop
  `BatteryOptimisationEffect`'s request branch and `requestBatteryExemption`; keep
  `backgroundRunIntent` and the vendor table. Likely renamed, since it no longer requests anything.
- `src/app/src/main/java/nl/hexmaster/pillsner/ui/home/HomeViewModel.kt` — remove
  `shouldRequestBatteryExemption` and `onBatteryExemptionRequested`; feed the banner from the
  missed-reminder signal instead of `batteryExempt`.
- `src/app/src/main/java/nl/hexmaster/pillsner/ui/home/HomeUiState.kt` — `batteryExempt` gives way to
  a "a reminder was missed" input in `remindersAreUnreliable`.
- `src/app/src/main/java/nl/hexmaster/pillsner/ui/PillsnerApp.kt` — the effect's call site.
- `src/app/src/main/java/nl/hexmaster/pillsner/data/reminders/ReminderPreferences.kt` — remove the
  asked-once flag; add the missed-reminder flag.
- The lapse path in the reminder domain — where a pending dose becomes missed — is where the signal
  is written.
- `src/app/src/main/res/values*/strings.xml` — the banner string is reworded, in every locale.

**Tests**

- `ReminderBannerTest` and the Home view-model tests change with the trigger.
- New coverage: a lapsed un-reminded dose raises the banner; a lapsed dose that *was* reminded does
  not; the flag clears.
- `ReminderNotifierTest`, `ReminderWakeTest`, `ReminderWatchdogTest`, `ReminderRecoveryTest` and the
  navigation tests are unaffected in behaviour but stop needing the whitelist precondition.

**Docs**

- `README.md` and `PRIVACY.md` if either lists the permission.
- `docs/design-system.md` is untouched; no token, colour or type role changes.

**Risk**

- On a device where an OEM really is killing the app, the user is now told after the first missed
  dose rather than before it. That is one dose of latency, against a prompt that fires for everyone
  including the large majority the platform was never going to throttle. The alternative — keeping a
  banner that is permanently true on most phones — trains the user to ignore it, which is worse.
