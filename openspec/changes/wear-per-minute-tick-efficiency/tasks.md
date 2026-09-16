## 1. WatchViewModel: single ticker

- [x] 1.1 Remove the `connectivity` flow in `wear/.../wear/ui/WatchViewModel.kt`; change `uiState` to `combine(payloads, minuteTicker) { payload, now -> ... }` and call `isPhoneConnected()` directly inside that transform.
- [x] 1.2 Confirm `isPhoneConnected` (currently `suspend () -> Boolean`) can be called directly inside the `combine` lambda without a separate coroutine; adjust its type only if required for the direct call to compile.
- [x] 1.3 Add/update a `WatchViewModel` unit test asserting `isPhoneConnected` is invoked exactly once per minute tick (covers the new "Single per-minute update source" spec requirement).

## 2. DoseCard: cached formatter

- [x] 2.1 In `wear/.../wear/ui/DoseCard.kt`, wrap the `DateTimeFormatter` construction in `timeText()` with `remember(locale, zone) { ... }`.
- [x] 2.2 Adjust `formatTime(instant, locale, zone)` (or add an overload) so the cached formatter can be passed in from the composable without breaking existing non-Compose callers/tests.
- [x] 2.3 Verify existing `DoseCard`/`formatTime` tests still pass unchanged (formatted output must be identical).

## 3. UpcomingWindowFilter: epoch-millis comparison

- [x] 3.1 In `wear/.../wear/domain/UpcomingWindowFilter.kt`, replace the per-dose `Instant.ofEpochMilli(...)` allocation with a raw `Long` comparison against `now.toEpochMilli() + WINDOW.toMillis()`.
- [x] 3.2 Re-run the existing window-boundary unit tests (exactly-6-hours-away dose, overdue dose ordering) to confirm identical results with the new comparison.

## 4. Spec and verification

- [x] 4.1 Confirm `openspec/changes/wear-per-minute-tick-efficiency/specs/wearable-app/spec.md`'s new "Single per-minute update source" requirement is satisfied by the test added in 1.3.
- [x] 4.2 Run the wear module's unit tests.
- [x] 4.3 Run lint for the wear module.
- [x] 4.4 Manually sanity-check the watch screen (or an emulator) for one full minute to confirm the list and connectivity footer still update on schedule with no visible change in behaviour.
