## Context

Four unrelated hot paths independently do more synchronous or duplicated work than they need to:

1. `KeystorePinVerifier.verify()` calls `isAvailable()` (which does `containsAlias` + `getKey`) and then `hmac()` → `loadOrCreateKey()` (which calls `getKey` again) — two Keystore round-trips per PIN check.
2. `UnlockWithPin`, `VerifyIdentity`, `ChangePin`, `IsCurrentPin`, `EnablePinLock` call that same blocking `PinVerifier` directly from `viewModelScope` (Main) with no dispatcher switch.
3. `PillsnerApplication.applyStoredLanguage()` does `runBlocking { languageRepository.observeLanguage().first() }` on every call. It is called once from `startWhenUnlocked()` (cheap, documented as "one small file, read once per process") and again from `onConfigurationChanged()`, which Android always invokes on the main thread — but per `LanguageSectionViewModel`'s own contract ("Choosing a language writes it and nothing else: it takes effect the next time the app starts"), the stored language cannot change during a running process, so this second read is pure waste on the UI thread.
4. `AppContainer.theme` builds its `StateFlow` by calling `themeRepository.observeTheme()` twice — once via `runBlocking { .first() }` for the synchronous initial value (a deliberate, documented trade-off to paint the correct theme on the first frame) and again as the upstream of `stateIn` — two independent collectors against the same DataStore Preferences flow at startup.

None of these are bugs; todo.md frames them correctly as optimisation opportunities. They're grouped here because they share a theme (redundant or blocking work during startup / security-sensitive verification) rather than because they touch the same files.

## Goals / Non-Goals

**Goals:**
- Remove the duplicate Keystore `getKey` call from every PIN check.
- Ensure PIN verification's Keystore/JCE work never runs on the main thread.
- Remove the redundant DataStore read from the locale-change path without changing when or how the stored language is applied.
- Collapse `AppContainer.theme`'s two DataStore subscriptions into one while preserving its documented guarantee (theme is correct from the very first frame).
- Zero observable behaviour change: PIN setup/verify/change/cooldown, biometric fallback, language switching, and theme painting all behave exactly as the `app-lock`, `app-language`, and `app-theme` specs already describe.

**Non-Goals:**
- Changing any app-lock, language, or theme requirement or user-facing flow.
- Introducing a DI framework change, a new dependency, or new persistence.
- Touching the recovery path (`ResolveInitialLockState` / `isAvailable()`), whose public contract and existing tests stay as-is.

## Decisions

**D1 — `KeystorePinVerifier`: fetch the key once per `verify()` call.**
Replace the `isAvailable()` + `hmac()` → `loadOrCreateKey()` sequence inside `verify()` with a single internal key fetch (e.g. a private `currentKey(): SecretKey?` that returns null if the alias is missing, used directly by both the availability check inlined into `verify()` and by the HMAC computation). The public `isAvailable()` method keeps its existing signature and behaviour (it's used by `ResolveInitialLockState` and has its own instrumented test), so it isn't removed — only `verify()`'s internal call graph changes to avoid calling through it and then fetching the key again.
*Alternative considered:* caching the `SecretKey` in a field across calls. Rejected — Keystore keys can be invalidated (e.g. lock-screen credential change) between calls, and the existing design deliberately re-resolves the key each time (see `loadOrCreateKey`'s KDoc); caching within a single `verify()` invocation is safe, caching across invocations is not.

**D2 — App-lock use cases: `withContext(Dispatchers.IO)` around `PinVerifier` calls.**
Each of `UnlockWithPin`, `VerifyIdentity`, `ChangePin`, `IsCurrentPin`, `EnablePinLock` wraps its call into `PinVerifier` (`verify`/`create`/`isAvailable`) in `withContext(Dispatchers.IO)`. This is a mechanical, per-use-case change with no shared abstraction needed — each use case is already a small suspend function.
*Alternative considered:* moving the dispatcher switch inside `KeystorePinVerifier` itself. Rejected — the domain layer must stay Android-framework-agnostic per CLAUDE.md, and `PinVerifier` is a domain interface; `Dispatchers.IO` usage belongs in the call sites that already own coroutine scoping, not in the data-layer implementation or the domain interface.

**D3 — `PillsnerApplication`: cache the stored language instead of re-reading on every configuration change.**
`startWhenUnlocked()`'s call to `applyStoredLanguage()` is the only place the stored language can actually be read as "current" during a process's lifetime (per `LanguageSectionViewModel`, a language change only takes effect on the next app start). Store the value read there in a field and have `onConfigurationChanged()` reapply that cached value via `AppLocale.apply(...)` directly, without touching `languageRepository` again. `applyStoredLanguage()` keeps doing the DataStore read only on its first (process-start) call.
*Alternative considered:* keeping the DataStore read but moving it off the main thread with a background dispatcher launched from `onConfigurationChanged()`. Rejected — `AppLocale.apply(...)` must run synchronously within `onConfigurationChanged()` for the new configuration to take effect for that callback; deferring it to a coroutine would reintroduce a race between the callback returning and the locale being applied. Caching avoids the disk read entirely, which is strictly better here.

**D4 — `AppContainer.theme`: single subscription feeding both the initial value and the hot flow.**
Collect `themeRepository.observeTheme()` through one coroutine in `containerScope` that publishes into a `MutableStateFlow`, and obtain the synchronous initial value by blocking (`runBlocking`) on the first emission of that same collection (e.g. via a `CompletableDeferred` completed the first time the collector runs), rather than calling `observeTheme()` a second time for `stateIn`'s `initialValue`. The public type stays `StateFlow<AppTheme>`, so no caller changes.
*Alternative considered:* leaving two `observeTheme()` calls but justifying it as cheap (as the existing KDoc already does for the DataStore-cache angle). Rejected because the same reasoning that made this worth listing in the backlog — no reason to pay for two independent flow subscriptions when the guarantee needed is "a correct value in place before the first frame" — applies regardless of how cheap DataStore's internal caching makes each read.

## Risks / Trade-offs

- [D1] A subtle ordering bug in the refactor could make `verify()` accept or reject a valid PIN incorrectly → mitigation: the existing `KeystorePinVerifierTest` instrumented tests (including the `isAvailable()` true/false cases) must keep passing unchanged, and a new test asserting `verify()` still succeeds/fails correctly after key rotation/invalidation should be added.
- [D2] Wrapping in `withContext(Dispatchers.IO)` inside a `@Synchronized`-adjacent or otherwise ordering-sensitive use case could change execution order → mitigation: none of the five use cases hold locks or depend on same-thread execution; existing unit tests for these use cases (which use a fake, non-blocking `PinVerifier`) must keep passing.
- [D3] Caching the language means a hypothetical future feature that changes language without an app restart would silently stop working on configuration change → mitigation: this is explicitly out of scope per `LanguageSectionViewModel`'s current contract; if that contract changes, this cache must be revisited (call this out in tasks.md as a follow-up note, not a blocker).
- [D4] A bug in the single-subscription rewiring could delay or drop the initial theme value, reintroducing the white-flash flicker the current code prevents → mitigation: this is exactly what `AppContainer.theme`'s KDoc says it protects against; manual verification (cold start in dark mode) is required in addition to any unit test, since `AppContainer` itself isn't unit-testable in isolation from Android context.

## Migration Plan

No data migration. This is a same-process code change with no schema, storage format, or API surface change. Roll out as a normal PR; rollback is a plain revert since no persisted state changes shape.

## Open Questions

None — all four decisions are self-contained and don't depend on unresolved product questions.
