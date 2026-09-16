## Why

A backlog scan (`docs/todo.md`) found two small, independent inefficiencies that don't share a root cause with any of the other grouped optimization changes (reminder wake-cycle DB efficiency, startup/PIN-verification blocking, Compose UI recomposition, wear per-minute tick efficiency): a per-call allocation in the `Quantity` domain model, and a dead version-catalog entry that leaves the wear module building against the phone's `minSdk` instead of its own. They're batched together here purely because each is too small to justify its own change, not because they're related.

## What Changes

- `domain/model/Quantity.kt` — cache the `normalizedValue` property (currently a `get()` that recomputes `value.stripTrailingZeros()` on every access, called from both `hashCode()` and `toString()`) instead of recomputing it each time. No behaviour change: `Quantity` still hashes and prints the same way, just without repeating the allocation. (`equals()` does not use `normalizedValue` — it already compares via `BigDecimal.compareTo`, which is scale-insensitive.)
- `wear/build.gradle.kts` — set `minSdk = libs.versions.wearMinSdk.get()` instead of `libs.versions.minSdk.get()`, so the wear module actually builds against the Wear OS 3+ floor (API 30) the version catalog already declares and the README's Toolchain table already documents (`minSdk` row already lists "26 (phone), 30 (Wear OS)"). **BREAKING** in the narrow sense that the wear APK will refuse to install on API 26-29 devices, but per the version catalog's own comment and the README, no such device is a supported Wear OS 3+ target — this fix aligns actual behaviour with already-documented intent rather than introducing new intent.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `wearable-app`: add a requirement documenting the minimum Wear OS platform version the companion app supports, so the already-intended API 30 floor (previously only recorded in the version catalog and the README's Toolchain table) is captured as a spec-level guarantee now that the build actually enforces it.

`medication-persistence` is unaffected — it does not document `Quantity`'s internal equality/hashing mechanics, only its externally observable behaviour, which is unchanged. No spec delta needed for that item.

## Impact

- `src/app/src/main/kotlin/.../domain/model/Quantity.kt` (or equivalent module path) — internal-only change, no callers affected.
- `src/wear/build.gradle.kts` — wear module's effective `minSdk` rises from 26 to 30.
- No dependency, permission, or network changes.
