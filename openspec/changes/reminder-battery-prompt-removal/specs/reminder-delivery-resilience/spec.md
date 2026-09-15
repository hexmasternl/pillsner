## REMOVED Requirements

### Requirement: Battery optimisation exemption is requested

**Reason**: The exemption buys Pillsner nothing it does not already have. Its only documented effects
are network access and partial wake locks during Doze; the app has no network permission and holds no
wake lock of its own. Reminders ride on `AlarmManager.setAlarmClock`, which the system leaves Doze to
deliver; `USE_EXACT_ALARM` already keeps the app out of the restricted standby bucket; and an exact
alarm is already an exemption from the foreground-service background-start restriction. The dialog
therefore interrupted every user with a system prompt in exchange for no measurable gain, and cost a
Play-policy-sensitive permission and an instrumented-test hazard besides. See design D1.

**Migration**: Nothing to migrate for the user. The app no longer opens the dialog, and
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` leaves the manifest, so the one-tap request intent is no
longer available. Users who are already exempt stay exempt; the platform does not revoke it. The
route to the setting survives as the Home banner's action, which opens the system
battery-optimisation list or the vendor auto-start screen. The stored "already asked" preference key
is abandoned in place; nothing reads it.

## ADDED Requirements

### Requirement: The app never requests the battery-optimisation exemption

Pillsner MUST NOT open any system dialog that the user did not ask for. In particular it MUST NOT
start `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, and MUST NOT declare
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

The app MAY read whether it is exempt, because reading is silent, but that state alone SHALL NOT
cause anything to be shown to the user.

#### Scenario: First upcoming dose on a non-exempt device

- **WHEN** the user saves their first scheduled medicine on a device where the app is not exempt from battery optimisation and opens Home
- **THEN** no system dialog appears and no banner is shown

#### Scenario: Returning to Home while not exempt

- **WHEN** the app is not exempt, reminders have all arrived, and the user opens Home any number of times
- **THEN** no system dialog appears and no banner is shown

#### Scenario: Manifest holds no battery permission

- **WHEN** the merged manifest is inspected
- **THEN** it declares no `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission

### Requirement: A silently missed reminder is recorded

When a pending dose lapses and is recorded as missed, the app SHALL record that a reminder was
silently missed if, and only if, all of the following hold:

- no reminder was ever posted for that dose;
- the dose was stored before its own due moment, so the app had a window in which to remind; and
- notifications were allowed, so the absence of a reminder is not explained by a denied permission.

The record SHALL survive process death and reboot, and SHALL carry the moment of the most recent such
miss.

#### Scenario: An alarm the platform did not deliver

- **WHEN** a dose planned yesterday for 08:00 today lapses with no reminder ever posted for it, and notifications are allowed
- **THEN** a silently missed reminder is recorded, with that dose's lapse moment

#### Scenario: A dose that was reminded and simply not answered

- **WHEN** a dose was reminded about and the user never answered, and it lapses
- **THEN** nothing is recorded; it is a missed dose, not a missed reminder

#### Scenario: A dose generated after its own moment

- **WHEN** the user saves a medicine at 20:00 whose schedule includes 08:00, and today's 08:00 dose is generated already lapsed
- **THEN** it is recorded as missed but no silently missed reminder is recorded

#### Scenario: Notifications were denied

- **WHEN** a dose lapses with no reminder posted because notifications are not allowed
- **THEN** no silently missed reminder is recorded

#### Scenario: Survives a restart

- **WHEN** a silently missed reminder has been recorded and the process is killed or the device reboots
- **THEN** the record is still present when the app next runs

## MODIFIED Requirements

### Requirement: Home reports when the system is throttling reminders

When a silently missed reminder has been recorded and not yet acknowledged, the Home screen SHALL
show a banner stating that a reminder did not arrive, with a button that opens a screen where the
user can stop it happening again. The banner SHALL NOT be raised by the battery-optimisation state
alone.

The record SHALL be cleared when the user activates the banner's button, and by the app reset. It
SHALL NOT be cleared merely because a later reminder posted successfully.

At most one reminder banner SHALL ever be shown. When more than one condition holds, the banner SHALL
report the most severe, in this order: notifications not allowed, then a reminder was silently
missed, then alarms not exact.

#### Scenario: A reminder was missed

- **WHEN** a silently missed reminder has been recorded, notifications are allowed and alarms are exact
- **THEN** Home shows the missed-reminder banner

#### Scenario: Not exempt but nothing has been missed

- **WHEN** the app is not exempt from battery optimisation and no reminder has been silently missed
- **THEN** Home shows no banner

#### Scenario: Acknowledged

- **WHEN** the user activates the banner's button and returns to Home
- **THEN** the banner is gone

#### Scenario: A later reminder arrives

- **WHEN** a silently missed reminder has been recorded, the user has not acknowledged it, and a later reminder posts successfully
- **THEN** the banner is still shown

#### Scenario: It happens again

- **WHEN** the user has acknowledged a missed reminder and a further dose later lapses un-reminded
- **THEN** the banner is shown again

#### Scenario: Notifications denied takes precedence

- **WHEN** notifications are not allowed and a silently missed reminder has also been recorded
- **THEN** only the notifications banner is shown

#### Scenario: A missed reminder takes precedence over inexact alarms

- **WHEN** a silently missed reminder has been recorded and alarms are also not exact
- **THEN** only the missed-reminder banner is shown

### Requirement: Vendor auto-start guidance on known devices

On devices whose manufacturer is known to stop background apps, the Home banner's button SHALL open
the vendor's own auto-start or protected-app screen. Each vendor intent MUST be checked for a
handling activity before it is offered, and when none resolves the button SHALL open the generic
system battery-optimisation list instead.

The choice of destination SHALL NOT depend on whether the app is currently exempt from battery
optimisation: a user who is exempt and still missing reminders is exactly the user the vendor screen
is for.

#### Scenario: Known vendor with a resolvable screen

- **WHEN** the device manufacturer is one of the known list, its auto-start screen resolves, and the user activates the banner's button
- **THEN** the vendor screen opens

#### Scenario: Known vendor whose screen no longer resolves

- **WHEN** the device manufacturer is on the list but its auto-start intent resolves to nothing
- **THEN** the system battery-optimisation list opens instead and the app does not crash

#### Scenario: Unknown vendor

- **WHEN** the device manufacturer is not on the list
- **THEN** the system battery-optimisation list opens

#### Scenario: Already exempt on a known vendor

- **WHEN** the app is already exempt from battery optimisation on a device whose vendor screen resolves
- **THEN** the vendor screen still opens
