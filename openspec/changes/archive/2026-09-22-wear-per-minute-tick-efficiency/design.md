## Context

`WatchViewModel` (`wear/.../wear/ui/WatchViewModel.kt`) drives the watch screen with `combine(payloads, minuteTicker, connectivity)`, where `minuteTicker` is a cold `flow { ... delay(...) }` that ticks once a minute. `connectivity` is declared as `minuteTicker.map { isPhoneConnected() }` — a *second* collector of the same cold flow. Because `minuteTicker` is never made hot (`shareIn`/`stateIn`), each collector runs its own independent `while(true) { emit; delay }` loop with its own clock reads and its own `delay` scheduling. `combine()` still only reacts once both upstreams have emitted, but the app now runs two concurrent per-minute loops instead of one, and each downstream computation keyed off the ticker (`entriesFor` → `UpcomingWindowFilter.filter` → per-dose mapping) fires once per tick of the *first* branch, while `isPhoneConnected()` is invoked once per tick of the *second* branch — doubling total tick-driven work for no functional benefit.

Two of the tick-driven computations also do avoidable per-call allocation:
- `DoseCard.kt`'s `formatTime()` builds a new `DateTimeFormatter.ofLocalizedTime(...)` every call; it's invoked from the `@Composable` `timeText()` on every recomposition of every visible `DoseCard`, which recomposes at least once per minute tick.
- `UpcomingWindowFilter.filter()` calls `Instant.ofEpochMilli(it.scheduledAtEpochMillis)` per dose per call, purely to compare against `until` (itself an `Instant`), when the comparison can be done on the raw epoch-millisecond `Long`s already stored on `SyncedDose` and computed once for `until`.

This is a Wear OS device where every tick this app runs competes for the same battery budget as the watch face and everything else on the wrist, so removing doubled and per-call-allocated work here is a real (if small) win, with no behavioural change: `wearable-app`'s six-hour window, footer, and entry-content requirements are all preserved exactly.

## Goals / Non-Goals

**Goals:**
- Collect the minute ticker exactly once per `WatchViewModel` instance.
- Avoid allocating a `DateTimeFormatter` on every `DoseCard` recomposition.
- Avoid allocating an `Instant` per dose on every `UpcomingWindowFilter.filter()` call.
- Preserve every existing `wearable-app` scenario byte-for-byte (window boundary, overdue ordering, tomorrow indication, footer timing).

**Non-Goals:**
- Changing the tick interval, the six-hour window size, or any displayed text/format.
- Introducing `shareIn`/`stateIn` or any new sharing operator — the simpler fix (call `isPhoneConnected()` inline) removes the second collector entirely, so no new hot-flow lifecycle needs to be reasoned about.
- Touching phone-side sync code (`WearDataClientFactory`, `DoseSyncPublisher`, etc.) — out of scope, and already reviewed as efficient in `docs/todo.md`.

## Decisions

**1. Fold `connectivity` into the `combine` transform instead of sharing `minuteTicker`.**
Replace the three-flow `combine(payloads, minuteTicker, connectivity)` with a two-flow `combine(payloads, minuteTicker)` whose transform calls `isPhoneConnected()` directly, e.g.:
```kotlin
val uiState: StateFlow<WatchUiState> = combine(payloads, minuteTicker) { payload, now ->
    WatchUiState(
        entries = payload?.let { entriesFor(it, now) }.orEmpty(),
        phoneConnected = isPhoneConnected(),
        hasData = payload != null,
        locale = payload?.languageTag?.let(Locale::forLanguageTag) ?: Locale.getDefault(),
    )
}.stateIn(...)
```
Considered `minuteTicker.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)` and having both `combine` inputs read the shared flow. Rejected: it still runs two `combine` reactions in lockstep with sharing overhead and a replay cache to reason about, for no benefit over simply calling the suspend function once inside the single transform already keyed off the ticker. Inlining is strictly simpler and removes the second coroutine entirely.

**2. Remember the `DateTimeFormatter` in `timeText()`, not inside `formatTime()`.**
`formatTime()` is a plain (non-`@Composable`) top-level function reused by other callers (e.g. tests), so caching can't live inside it without changing its signature or making it stateful. Instead, wrap the formatter construction in `timeText()` (the `@Composable` call site) with `remember(locale, zone) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).withZone(zone) }` and pass the cached formatter into `formatTime`, or overload `formatTime` to accept a pre-built `DateTimeFormatter`. This keeps `formatTime(instant, locale, zone)` usable and testable as-is for anyone calling it outside composition, while the hot Composable path stops rebuilding the formatter every recomposition.

**3. Compare raw epoch millis in `UpcomingWindowFilter`.**
Compute `val untilMillis = now.toEpochMilli() + WINDOW.toMillis()` once per `filter()` call, then filter with `it.scheduledAtEpochMillis <= untilMillis` — no per-dose `Instant` allocation. `now` is still accepted as `Instant` (unchanged public signature) since callers already have one; only the internal comparison changes.

## Risks / Trade-offs

- [Risk] Removing the second `minuteTicker` collector changes emission timing subtly (previously two independent `delay` schedules could interleave; now there's exactly one). → Mitigation: this can only make ticks *more* synchronized/predictable, never less — `combine` was already gated on both branches ticking, so the visible cadence (once per minute) is unchanged. Cover with the existing/added `WatchViewModel` unit test asserting one recomposition-worth of state per minute tick.
- [Risk] Formatter caching via `remember(locale, zone)` must key on both `locale` and `zone`, or a language/timezone change on the phone could show a stale format. → Mitigation: explicitly key `remember` on both values, matching how other `DateTimeFormatter` usages elsewhere in the app already do (per `docs/todo.md`'s "reviewed, no issues" list).
- [Risk] Epoch-millis comparison must use the same window arithmetic as before (`now.plus(WINDOW)` inclusive of exactly-on-boundary doses per the "Window boundary" scenario). → Mitigation: `WINDOW.toMillis()` addition is equivalent to `Instant.plus(Duration)` for millisecond-precision `SyncedDose` timestamps; keep the existing `<=` (not `<`) comparison and cover with the existing boundary unit test.

## Migration Plan

No data migration. Roll out as a normal app update to the wear module; if an issue surfaces, revert the three commits (no schema, API, or persisted-state changes are involved).

## Open Questions

None.
