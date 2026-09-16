GitHub issue: [#6](https://github.com/hexmasternl/pillsner/issues/6)

## Why

A backlog scan (`docs/todo.md`) found that app startup and PIN verification do more synchronous, blocking, and redundant work than necessary: PIN checks hit the Android Keystore twice per attempt, several app-lock use cases call blocking Keystore/JCE crypto directly on `viewModelScope` (Main) with no `withContext(Dispatchers.IO)`, a locale change blocks the main thread on a `runBlocking` DataStore read in `onConfigurationChanged()`, and the app's theme `StateFlow` subscribes to the same DataStore flow twice at startup. None of this is a functional bug — behaviour is correct today — but it risks main-thread jank on every unlock attempt, PIN change, biometric fallback, and system locale change, and does needless duplicate work at process start. Fixing it now, before more app-lock flows are added, keeps PIN verification and startup responsive.

## What Changes

- `applock/data/KeystorePinVerifier.kt`: `verify()` fetches the Keystore key once and reuses it for both the availability check and the HMAC, instead of two separate `keyStore.getKey(...)` round-trips.
- `applock/domain/UnlockWithPin.kt`, `VerifyIdentity.kt`, `ChangePin.kt`, `IsCurrentPin.kt`, `EnablePinLock.kt`: wrap their `PinVerifier` calls in `withContext(Dispatchers.IO)` so Keystore/HMAC crypto never runs on the main thread.
- `PillsnerApplication.kt`: `applyStoredLanguage()` no longer performs a blocking DataStore read directly on the `onConfigurationChanged()` main-thread callback; the last-applied language is cached/derived so a locale change doesn't block the UI thread.
- `di/AppContainer.kt`: the `theme` `StateFlow` derives its initial blocking value and its hot flow from a single subscription to `themeRepository.observeTheme()` instead of two independent collectors.
- No new dependencies, no new permissions, no change to what the user sees or how PIN/lock/theme/language behave — this is an internal efficiency and main-thread-safety refactor only.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `app-lock`: adds a responsiveness guarantee — PIN verification SHALL NOT block the main thread, so the unlock/change-PIN/identity-check UI stays responsive while a PIN is checked. No change to any existing PIN, biometric, cooldown or recovery requirement.
- `app-language`: adds a guarantee that a system locale change re-applies the already-stored language without a visible stall, since the stored language cannot change mid-process. No change to the existing language-selection or restart-required behaviour.

`app-theme`'s existing requirement ("The chosen theme survives a restart and is applied before the first frame") already fully covers the guarantee D4 in design.md preserves, so no spec change is needed there.

## Impact

- Affected code: `applock/data/KeystorePinVerifier.kt`, `applock/domain/UnlockWithPin.kt`, `applock/domain/VerifyIdentity.kt`, `applock/domain/ChangePin.kt`, `applock/domain/IsCurrentPin.kt`, `applock/domain/EnablePinLock.kt`, `PillsnerApplication.kt`, `di/AppContainer.kt`.
- No API, dependency, schema, or permission changes.
- Risk is low but touches security-sensitive PIN verification code, so unit tests covering `KeystorePinVerifier` and the app-lock use cases must keep passing unchanged, and lint must be clean, before this is considered done.
