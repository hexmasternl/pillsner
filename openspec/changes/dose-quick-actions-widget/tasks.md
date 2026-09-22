## 1. Dependency and scaffolding

- [ ] 1.1 Add `androidx.glance:glance-appwidget` to the version catalog, pinned to its current stable release
- [ ] 1.2 Create the widget package (e.g. `ui/widget/`) with a `GlanceAppWidget` and `GlanceAppWidgetReceiver`, and register the widget provider in the manifest with its metadata (min size, resize mode, preview)

## 2. Widget state

- [ ] 2.1 Define the widget's Glance state (next dose / due dose / nothing due / masked-locked) as a small domain-facing data model, mapped from the existing dose repository/Flow — no new query logic
- [ ] 2.2 Wire state updates so `updateAll`/`update` is invoked after: a dose is answered (app or notification), a reminder is posted, and the planning-window refresh runs — with no periodic polling anywhere in the widget code
- [ ] 2.3 Read the app lock's enabled/disabled state and apply the masked rendering whenever it's enabled, per the `dose-quick-actions-widget` spec

## 3. Widget content

- [ ] 3.1 Build the "next dose" layout: medicine name, amount, scheduled time, no actions
- [ ] 3.2 Build the "due/overdue dose" layout with the three actions ("I took it", "Not yet", "Not going to"), matching the reminder notification's labels, order and string resources
- [ ] 3.3 Build the "nothing due" layout and the masked-while-locked layout
- [ ] 3.4 When multiple doses are due at once, select only the soonest-due one for display
- [ ] 3.5 Apply Pillsner design-system colours, type and the monochrome mark within Glance's theming support; run `pillsner-ui-review` against the widget's layouts

## 4. Answer actions

- [ ] 4.1 Implement a Glance `ActionCallback` for each of the three actions that calls the same domain use case the notification's `BroadcastReceiver` already calls, with no duplicated recording logic
- [ ] 4.2 Verify "Not yet" from the widget produces the identical snooze/repeat-reset behaviour as "Not yet" from the notification, and that it removes/updates the notification consistently
- [ ] 4.3 Verify answering from the widget updates any currently-shown reminder notification for the same dose, and vice versa

## 5. Tests

- [ ] 5.1 Unit test the widget's state-mapping logic (next dose selection, due/overdue detection, soonest-of-several selection, nothing-due detection) against the domain dose model, with no Android dependency
- [ ] 5.2 Unit test that the masked/locked state never includes medicine name, amount, or actions
- [ ] 5.3 Instrumented test: tapping each widget action records the same intake outcome as the equivalent notification action, and updates the notification/widget consistently
- [ ] 5.4 Instrumented test: the widget updates after an answer or a new reminder without any manual refresh, and does not update on any timer in the absence of such an event

## 6. Verification

- [ ] 6.1 Run unit tests, lint, and instrumented tests (widget and notification/reminder code changed)
- [ ] 6.2 Confirm no new permission was added and no network access was introduced
- [ ] 6.3 Update `README.md`'s feature list / Toolchain table if the new dependency needs to be reflected there
