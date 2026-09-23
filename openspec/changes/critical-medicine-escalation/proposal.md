**GitHub Issue:** #51 (https://github.com/hexmasternl/pillsner/issues/51)

## Why

Every medicine today escalates its unanswered reminder on the same cadence, regardless of how much a missed dose matters. For a heart or seizure medication, that is not always enough: the user may want a shorter re-ask interval, more re-asks, and a more insistent alert before the dose lapses. Letting the user flag specific medicines as critical, opt-in and per medicine, keeps the app's "remind reliably" promise proportionate to what actually matters to that person, without changing anything for a medicine they have not flagged.

## What Changes

- `Medication` gains an optional `critical` flag, off by default.
- The Add medicine and Medicine details forms gain a "This is a critical medicine" toggle, off by default, alongside the existing fields.
- The Medicines screen tile and the dose detail screen show a critical badge (icon plus text, never colour alone) when the medicine is flagged.
- A critical medicine's reminder escalates on a distinct, faster cadence: it re-asks every 5 minutes (instead of every 15) up to eight times (instead of four), and uses a distinct, more insistent sound/vibration pattern from the first post, before falling back to the ordinary lapse rule (next dose due, or 24 hours, whichever comes first — unchanged).
- The three recorded outcomes (taken, skipped, missed) and the three answers (I took it, Not yet, Not going to) are unchanged; only the pace and insistence of asking change.
- The paired Wear OS watch mirrors the critical badge on its list entry, consistent with every other reminder detail it already mirrors.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `medication-schedule-model`: `Medication` gains a `critical` boolean flag.
- `medicine-add`: the add form gains the critical toggle field, off by default.
- `medicine-details`: the details form gains the same toggle, editable like any other field.
- `medicine-overview`: medicine tiles show a critical badge, distinguished by icon and text, not colour alone.
- `dose-detail`: the dose detail screen shows the same critical badge for a critical medicine's dose.
- `medicine-reminders`: a critical dose's reminder re-asks every 5 minutes up to eight times, on a distinct, more insistent notification channel, instead of the standard 15-minute, four-times cadence; the snooze interval and its repeat-reset behaviour follow the same critical cadence.
- `wearable-sync`: the sync payload carries the critical flag per dose.
- `wearable-app`: the watch list entry shows the critical badge when its dose's medicine is flagged.

## Impact

- Room: `Medication` schema gains a `critical` column with a default of `false`; ships with a migration and a migration test.
- No new dependency, no new permission. Escalation stays within the app's existing exact-alarm-driven reminder path — no new delivery channel, no bypass of the alarm-clock tier.
- `reminder-delivery-resilience`'s guarantees (silently-missed detection, watchdog repair, banners) are unaffected: they operate on whichever cadence is currently armed, critical or not.
