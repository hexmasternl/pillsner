GitHub issue: [#7](https://github.com/hexmasternl/pillsner/issues/7)

## Why

The Wear OS companion app re-derives its upcoming-doses list every minute via a ticker flow, but `WatchViewModel` accidentally runs that ticker twice concurrently, doubling `UpcomingWindowFilter` and list-mapping work, and two of its collaborators (`DoseCard.formatTime()`, `UpcomingWindowFilter`) allocate a new formatter/`Instant` on every one of those calls instead of reusing one. On a battery- and CPU-constrained watch this is wasted work every minute the screen is open, with no user-visible benefit. Fixing the doubled ticker also halves how often the two allocation issues fire, so all three are worth fixing in one pass.

## What Changes

- `wear/.../wear/ui/WatchViewModel.kt`: stop deriving `connectivity` from a second `minuteTicker.map { isPhoneConnected() }` subscription; call `isPhoneConnected()` directly inside the existing `combine()` transform so the minute ticker is only collected once.
- `wear/.../wear/ui/DoseCard.kt`: cache the `DateTimeFormatter` built in `formatTime()` with `remember(locale, zone)` instead of constructing a new one on every call from `timeText()`.
- `wear/.../wear/domain/UpcomingWindowFilter.kt`: compare raw epoch-millisecond `Long`s instead of allocating a new `Instant.ofEpochMilli(...)` per dose on every filter call.
- No behaviour, timing, or on-screen content changes — this is an internal efficiency fix only. The six-hour window, footer timing, and list ordering all stay exactly as specified today.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `wearable-app`: adds one new non-functional requirement locking in that per-minute updates (list recomputation and connectivity check) come from a single timer, so the duplicate-ticker regression this change fixes can't silently return. No existing requirement's behaviour changes — the six-hour window, footer timing, entry content, and all other `wearable-app` requirements are unaffected.

## Impact

- Affected files: `wear/src/main/kotlin/.../wear/ui/WatchViewModel.kt`, `wear/src/main/kotlin/.../wear/ui/DoseCard.kt`, `wear/src/main/kotlin/.../wear/domain/UpcomingWindowFilter.kt`.
- No API, dependency, schema, or permission changes.
- Reduces per-minute CPU/allocation work on the watch (halves ticker-driven work, removes a formatter and an `Instant` allocation per dose per tick).
