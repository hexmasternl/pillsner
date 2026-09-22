**GitHub Issue:** #31 (https://github.com/hexmasternl/pillsner/issues/31)

## Why

Pillsner's promise is "confirm in one tap," but today that tap only exists once a reminder notification has already fired. Between reminders the user has to open the app just to see what's next or to act on a dose that's already due. A home-screen widget closes that gap using only first-party platform APIs (Jetpack Glance), with no new permission and no network access.

## What Changes

- A new home-screen widget, built with Jetpack Glance, that shows the next pending dose (name, amount, scheduled time) or, when a dose is currently due or overdue, shows that dose instead with the same three answer actions the reminder notification already offers: "I took it", "Not yet", "Not going to".
- Tapping an action on the widget records the same intake outcome the notification action would (taken, skipped, or a snooze that reappears at the dose's next repeat), through the same recording path the notification uses today — no parallel logic.
- When nothing is pending, the widget shows an explicit "nothing due" message instead of an empty tile.
- The widget respects the app lock: while a PIN/biometric lock is enabled, the widget shows only that something is due, without the medicine name or amount, and offers no answer actions — mirroring how the lock-screen notification already withholds content. (Widget content cannot itself be gated behind unlock, since Android renders widgets outside the app's own locked/unlocked state.)
- The widget refreshes by observing the same on-device dose data the app and notifications already read (via a `GlanceStateDefinition` backed by the existing dose repository/Flow), and is pushed a new state whenever a dose is answered, reminded, or the day's doses are recomputed. It does not poll on a timer and it does not itself schedule, re-schedule, or fire any reminder.
- **Out of scope for this change**: the Quick Settings tile. The issue itself calls it "stretch scope, may ship separately"; this change ships the home-screen widget only, and a `TileService` tile is left for a follow-up change so its OEM-specific behaviour can be scoped on its own.

## Capabilities

### New Capabilities

- `dose-quick-actions-widget`: the home-screen widget itself — its content states (next dose, due/overdue with actions, nothing due, locked), its refresh triggers, and its answer actions.

### Modified Capabilities

- `medicine-reminders`: the "Three actions", "I took it", "Not yet snoozes for 15 minutes", and "Not going to skips" requirements gain an explicit guarantee that answering from the widget records the same outcome, through the same recording path, as answering from the notification — so a widget answer and a notification answer can never disagree about a dose.

## Impact

- Affected code: a new `ui/widget/` (or equivalent Glance) package, plus the existing dose-answering use case gains a second caller (the widget's action callback) alongside the notification's broadcast receiver.
- New first-party dependency: `androidx.glance:glance-appwidget` (AndroidX, no third-party library, no network access) — added to the version catalog.
- No schema change, no new permission, no navigation change to the app itself.
- Must go through `pillsner-ui-review` for the widget's visual content (it still carries the Pillsner mark, colours and type from the design system, adapted to Glance's more limited theming).
