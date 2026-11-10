---
name: kotlin-conventions
description: How Kotlin is written in Pillsner beyond what lint and the formatter settle - modelling with sealed types, value classes and immutability, null safety and expected failures as values, java.time with an injected Clock, structured concurrency and Flow with injected dispatchers, layering and visibility, and privacy-safe logging. Use whenever writing or reviewing Kotlin in src/app, src/wear or src/shared - by pillsner-kotlin-expert for all its code, by pillsner-designer for view-model-facing UI code, and by the critics when judging a leaky contract or a time-handling bug.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

Follow the official Kotlin coding conventions and the Android Kotlin style guide; lint and the formatter settle layout. Beyond that:

**Types and modelling**
- Model the domain with the glossary's words: `Medication`, `Schedule`, `Dose`, `Intake`, `Reminder`, `Snooze`, `Adherence`, `Refill`.
- Make illegal states unrepresentable: `sealed interface` / `sealed class` for closed sets of outcomes and UI states, `enum class` for plain choices, `@JvmInline value class` for IDs and quantities that must not be swapped. Exhaustive `when` without an `else` branch on sealed types, so a new case breaks the build instead of falling through.
- Immutable by default: `val`, `data class` with `copy`, read-only `List`/`Map` in public signatures. Mutable state stays private and is exposed as `StateFlow`.
- Validate at construction (`init { require(...) }`) or in a factory returning a result type, so invalid values never travel.

**Null safety and errors**
- No `!!`. Use `?.`, `?:`, `requireNotNull`/`checkNotNull` with a message where absence is a programming error, and nullable types only where absence is meaningful.
- No `lateinit` outside framework-injected fields.
- Expected failures (not found, invalid input, permission missing) are values: a sealed result or a nullable return. Exceptions are for bugs and for I/O that genuinely failed. Never swallow `CancellationException`; when catching broadly in a coroutine, rethrow it.

**Time**
- `java.time` only. Store instants (`Instant`); compute "when is the dose due" from `LocalDate`/`LocalTime` plus a `ZoneId` at the moment of scheduling. Never do date arithmetic on epoch millis.
- Inject a `java.time.Clock` (or a small domain clock interface) wherever "now" matters; never call `Instant.now()`, `System.currentTimeMillis()` or `LocalDate.now()` without one.
- Write KDoc for every function whose result depends on time zone, DST or ordering, stating the zone and what happens in a gap or overlap.

**Coroutines and Flow**
- Structured concurrency: `viewModelScope`, a scope owned by a lifecycle, or `goAsync()` plus a bounded scope in receivers. No `GlobalScope`, no `runBlocking` in production code.
- Inject `CoroutineDispatcher`s rather than hard-coding `Dispatchers.IO`, so tests can use `StandardTestDispatcher`.
- `suspend` for one-shot work, `Flow` for streams. Expose UI state with `stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial)`. Cold flows in the data layer.
- No callbacks where a suspend function will do; wrap callback APIs with `suspendCancellableCoroutine` or `callbackFlow`.

**Structure**
- Layers stay separate: UI → domain (use cases, models; no Android imports) → data (repositories, Room, alarms). Dependencies point inwards and are wired in `AppContainer` / `WearContainer`; do not add a DI framework.
- Small files, small functions, explicit names. Prefer a plain function over an extension unless the receiver is the obvious subject; prefer composition over inheritance; avoid scope-function chains that hide what is happening (`let`/`also`/`run` nested more than one level deep).
- `internal` for anything not used outside its module; the narrowest visibility that compiles.
- No new library without an accepted proposal naming why (`dependency-bump` skill); versions only in `src/gradle/libs.versions.toml`.

**Privacy and logging**
- Never log a medication name, amount or note at info level or above. No network, no analytics, no third-party SDKs.

