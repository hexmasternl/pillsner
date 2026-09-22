## ADDED Requirements

### Requirement: Widget shows the next pending dose
The home-screen widget SHALL display the soonest pending dose that is not yet due: its snapshot medicine name, amount and scheduled time, in the same wording the Medicines and Home screens already use for a dose. The widget MUST NOT show answer actions for a dose that is not yet due.

#### Scenario: Next dose later today
- **WHEN** the soonest pending dose is scheduled for 18:00 and it is currently 09:00
- **THEN** the widget shows that dose's name, amount and 18:00, with no answer actions

#### Scenario: Widget updates as time passes
- **WHEN** the dose shown by the widget becomes due
- **THEN** the widget switches to the due-dose state without the user opening the app

### Requirement: Widget shows a due or overdue dose with answer actions
When a dose has reached its scheduled moment and has no intake yet, the widget SHALL show that dose instead of the next upcoming one, together with the same three actions as the reminder notification: "I took it", "Not yet", "Not going to", each usable directly from the widget without opening the app.

#### Scenario: Dose falls due
- **WHEN** the dose the widget is showing reaches its scheduled moment
- **THEN** the widget shows "I took it", "Not yet" and "Not going to" for that dose

#### Scenario: Overdue dose
- **WHEN** a dose is still pending 40 minutes after its scheduled moment
- **THEN** the widget continues to show it with its three actions until it is answered or lapses

### Requirement: Widget answer actions match the notification's outcomes
Tapping "I took it" on the widget SHALL record the dose as taken at the moment of the tap. Tapping "Not going to" SHALL record it as skipped. Tapping "Not yet" SHALL have the same snooze effect the reminder notification's "Not yet" action has, including resetting the dose's repeat sequence and never extending past the dose's lapse moment. Each action MUST go through the same recording path the reminder notification's actions use, so a dose answered from the widget is indistinguishable from one answered from the notification.

#### Scenario: Taken from the widget
- **WHEN** the user taps "I took it" on the widget at 08:12 for a dose scheduled at 08:00
- **THEN** the dose's intake is taken with recorded moment 08:12, matching what tapping the notification's "I took it" would have recorded

#### Scenario: Snoozed from the widget
- **WHEN** the user taps "Not yet" on the widget at 08:00
- **THEN** the dose is not recorded, the reminder notification for it (if already shown) continues to follow its normal 15-minute snooze cadence, and the widget shows the dose as still due until it is next reminded or answered

#### Scenario: Answering from the widget updates the notification
- **WHEN** a reminder notification for a dose is showing and the user taps "I took it" on the widget instead
- **THEN** the notification for that dose is removed, exactly as if it had been answered from the notification itself

### Requirement: Widget shows a "nothing due" state
When there is no pending dose that is due or overdue and no upcoming dose to show, the widget SHALL display an explicit message that nothing is due, sourced from a string resource, instead of an empty tile.

#### Scenario: No doses at all
- **WHEN** the user has no active medicines with schedules
- **THEN** the widget shows the "nothing due" message

#### Scenario: All doses answered
- **WHEN** every dose for the day has already been answered and none is yet scheduled
- **THEN** the widget shows the "nothing due" message

### Requirement: Widget masks content while the app lock is enabled
While the app's PIN or biometric lock is enabled, the widget SHALL NOT display any medicine name, amount or answer actions, regardless of whether a dose is due, since Android renders widgets outside the app's own locked state and cannot itself prompt for the PIN. It SHALL instead show a generic message stating only whether something is due, from a string resource, mirroring the wording the locked reminder notification already uses.

#### Scenario: Lock enabled, dose due
- **WHEN** the app lock is enabled and a dose is currently due
- **THEN** the widget shows only that something is due, with no name, amount or actions

#### Scenario: Lock enabled, nothing due
- **WHEN** the app lock is enabled and no dose is due
- **THEN** the widget shows the generic "nothing due" message

#### Scenario: Lock disabled
- **WHEN** the app lock is disabled
- **THEN** the widget shows full dose details as specified above

#### Scenario: Enabling the lock refreshes an already-shown widget
- **WHEN** the app lock is turned on while the widget is currently showing full dose details
- **THEN** the widget refreshes to the masked state immediately, without waiting for a dose to be answered or a reminder to post

### Requirement: Widget answer actions are rejected once the app lock is enabled
A widget's answer action MUST re-check the app lock's current enabled/disabled state at the moment it is invoked, not rely on whatever the widget last rendered. A launcher may retain a stale copy of the widget's `RemoteViews` from before the lock was enabled; its action `PendingIntent`s remain callable even after the widget has been told to mask itself.

#### Scenario: A stale widget cannot answer a dose once the lock is enabled
- **WHEN** the app lock is enabled after the widget last rendered full dose details, and the retained action is then invoked
- **THEN** the action does not record an outcome, and the widget refreshes to the masked state

### Requirement: Widget shows the single soonest dose when several are due
When more than one dose is due or overdue at once, the widget SHALL show only the soonest-due one of them, keeping the widget's layout fixed-size, consistent with the app not needing a scrolling list at this scale.

#### Scenario: Two doses due at once
- **WHEN** two medicines are simultaneously due
- **THEN** the widget shows the one whose scheduled moment is earliest, and the other remains reachable from the app and its own notification

### Requirement: Widget refresh is event-driven
The widget SHALL refresh only in response to a dose being answered, a reminder being posted, or the day's planning window being recomputed — the same events that already change what the app and the reminder notifications show. The widget implementation MUST NOT poll for dose state on a timer or periodic background job.

#### Scenario: Answered elsewhere
- **WHEN** a dose is answered from the app or from the reminder notification
- **THEN** the widget reflects the new state without the user needing to tap or refresh it

#### Scenario: No periodic polling
- **WHEN** the widget's implementation is inspected
- **THEN** it contains no periodic timer or `WorkManager` periodic job that queries dose state independently of the events above
