## Context

Two unrelated backlog findings from `docs/todo.md` are batched into one change purely because each is too small to justify its own proposal:

1. `Quantity.normalizedValue` (`src/app/.../domain/model/Quantity.kt:35`) is a computed property that calls `value.stripTrailingZeros()` on every access. It's read from `hashCode()` (line 43) and `toString()` (line 46), so every hash or debug-print allocates a fresh `BigDecimal`. `Quantity` instances are compared and hashed pervasively (`distinct()`, `Set` membership on medication/schedule lists), so this is a small but frequent allocation.
2. `wear/build.gradle.kts:35` sets `minSdk = libs.versions.minSdk.get()` (26, the phone floor) instead of `libs.versions.wearMinSdk.get()` (30). The catalog entry (`gradle/libs.versions.toml:14`) and the README's Toolchain table (`minSdk` row: "26 (phone), 30 (Wear OS)") already document 30 as the intended Wear OS floor — the build script just never wired it up. `wearMinSdk` is currently a dead catalog entry.

## Goals / Non-Goals

**Goals:**
- Stop `Quantity` from reallocating its normalized value on every `hashCode()`/`toString()` call.
- Make `wear/build.gradle.kts` build against the `minSdk` the project already documents as the Wear OS floor.

**Non-Goals:**
- Changing `Quantity`'s equality or hashing semantics (still scale-insensitive, still unit + numeric value).
- Auditing other version-catalog entries for similar drift — this change only fixes the one already found.
- Any UI, scheduling, or persistence behaviour change.

## Decisions

- **Cache `normalizedValue` as a lazily-initialized `val`** (`by lazy { value.stripTrailingZeros() }`) rather than a plain `val` computed in `init`. `Quantity` is immutable (`value` and `unit` are both `val`), so a `lazy` delegate is safe and avoids doing the `stripTrailingZeros()` work at all for instances whose hash/string form is never requested (e.g. transient intermediate values). A plain eagerly-computed `val` was considered and rejected only because `lazy`'s laziness is strictly better here at effectively the same cost (single volatile check vs. always paying the allocation).
- **Point `wear/build.gradle.kts` at `libs.versions.wearMinSdk`** instead of introducing a new catalog key — the catalog entry already exists and already carries the comment explaining why Wear OS needs API 30. No catalog change needed, only the one line in `wear/build.gradle.kts`.
- **No README change** — the Toolchain table already lists "26 (phone), 30 (Wear OS)", so fixing the build script brings reality in line with what's already documented rather than requiring a doc update.

## Risks / Trade-offs

- [Raising the wear module's effective `minSdk` from 26 to 30 could, in theory, break an install on an existing Wear OS device running API 26-29] → Mitigation: Wear OS 3 (the platform baseline for API 30) shipped in 2021 and is the version catalog's and README's already-documented floor; no supported device runs below it. Flagged here for visibility rather than treated as a real compatibility risk.
- [`by lazy` adds a synchronization check on first access] → Mitigation: negligible (`LazyThreadSafetyMode.SYNCHRONIZED` default is fine for a rarely-contended single field); can switch to `LazyThreadSafetyMode.NONE` if `Quantity` is confirmed single-thread-confined, but that's a micro-optimization not needed to fix the reported issue.

## Migration Plan

No data migration. Both changes are recompiled/rebuilt on the next build; no runtime migration or staged rollout needed. Rollback is a plain revert of both file changes.

## Open Questions

None.
