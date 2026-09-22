## Context

The app already answers a dose from two places: the reminder notification's three actions, and the in-app dose screens. Both go through the same domain use case that records an `Intake`. The widget adds a third caller of that same use case; it introduces no new way of deciding what's due — it only reads the dose data the rest of the app already computes and displays it, and it reuses the exact recording path the notification uses so the three surfaces (app, notification, widget) can never disagree about a dose's outcome.

Glance (`androidx.glance:glance-appwidget`) is AndroidX's Compose-like API for building widgets: layouts are written as composables but rendered through `RemoteViews` under the hood, so widget content updates by pushing new Glance state rather than by the widget process itself polling.

## Goals / Non-Goals

**Goals:**
- Show the next pending dose, or the currently due/overdue dose with answer actions, on the home screen without opening the app.
- Reuse the existing dose-answering use case and existing dose data — no new decision logic about what's due or when.
- Keep the widget's refresh event-driven (pushed on dose changes), never a polling loop.
- Respect the app lock by withholding medicine name/amount and actions while locked.

**Non-Goals:**
- The Quick Settings tile (explicitly deferred to a follow-up change per the proposal).
- Any change to reminder scheduling, repeat cadence, or notification behaviour — the widget is a second window onto the same state, not a second scheduler.
- Multi-widget-instance configuration (e.g. per-medicine widgets). One widget type showing the next/overdue dose across all medicines, matching the issue's scope.

## Decisions

- **Glance over a classic `RemoteViews`/`AppWidgetProvider` implementation.** Glance is the current first-party AndroidX widget API, keeps the widget's layout code close to the rest of the Compose UI (shared string resources, shared design-system colour tokens where Glance's `GlanceTheme` supports them), and is the API CLAUDE.md's "prefer AndroidX and Kotlin first-party libraries" rule points to. A raw `RemoteViews` implementation was considered and rejected as strictly more code for the same result.
- **State source: a `GlanceStateDefinition` fed by the same Flow the Home screen's view model already collects**, not a separate query. The widget's update worker (a `GlanceAppWidget` triggered via `updateAll`) is invoked from the same place that already recomputes doses after an answer, a reminder post, or the daily planning-window refresh — never on its own timer. This satisfies CLAUDE.md's rule against replacing exact alarms with inexact background work: the widget never drives scheduling, it is only ever a downstream observer that gets nudged after the real state changes.
- **Actions call the existing dose-answering use case directly**, via a Glance `ActionCallback` that runs in the widget's own process scope and invokes the same domain function the notification's `BroadcastReceiver` calls. No new recording path, no duplicated business rules about snooze bounds or repeat resets.
- **Lock behaviour: content-level masking, not a widget-level lock screen.** Android widgets render on the home screen regardless of the app's own in-app lock state, so Pillsner cannot show a PIN prompt inside the widget. Instead, when the app lock is enabled, the widget always renders its masked state ("Something is due" / "Nothing due right now", no name, no amount, no actions) regardless of whether a dose is actually due — matching the existing lock-screen notification's public text, and erring toward under-sharing rather than leaking medication details to anyone who can see the user's home screen.
- **Single widget, no per-medicine configuration**, keeping this change's scope to what issue #31 asked for and avoiding a widget-configuration screen that isn't otherwise needed.

## Risks / Trade-offs

- [Glance updates can be visibly delayed by the OS on some OEM launchers] → Mitigation: the widget is explicitly a glanceable secondary surface, not a reminder-delivery path; the notification remains the authoritative, timely alert, and the proposal and spec both state the widget only reflects state.
- [Masking widget content while locked means a locked user gets less use out of the widget] → Mitigation: this matches the existing, already-accepted trade-off the lock-screen notification makes, so it's a consistent product decision rather than a new one.
- [A third caller of the dose-answering use case increases the surface that must stay in sync with future changes to that use case] → Mitigation: the widget and the notification action share the exact same use-case call rather than each re-implementing "what happens on I took it," so a future change to that logic only needs to change one place.

## Open Questions

None outstanding — the proposal's "Open questions" (refresh strategy, tile scope, multiple-doses handling) are resolved above: event-driven refresh, tile deferred, and multiple simultaneous doses show the single soonest one (see spec) to keep the widget's fixed-size layout simple, consistent with how the wearable app's own compact surface already picks the next entry rather than listing every due dose.
