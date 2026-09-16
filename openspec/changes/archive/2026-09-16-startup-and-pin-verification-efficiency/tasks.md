## 1. Keystore PIN verification (D1)

- [x] 1.1 Refactor `KeystorePinVerifier.verify()` to fetch the Keystore key once and reuse it for the HMAC, instead of calling `isAvailable()` and then `loadOrCreateKey()` separately. Keep `isAvailable()`'s public signature and behaviour unchanged.
- [x] 1.2 Confirm existing `KeystorePinVerifierTest` instrumented tests pass unchanged (including both `isAvailable()` cases); add a case that exercises `verify()` after the key has been freshly created, to confirm the single-fetch path is correct.

## 2. Move PIN verification off the main thread (D2)

- [x] 2.1 Wrap the `PinVerifier` call in `UnlockWithPin` in `withContext(Dispatchers.IO)`.
- [x] 2.2 Wrap the `PinVerifier` call in `VerifyIdentity` in `withContext(Dispatchers.IO)`.
- [x] 2.3 Wrap the `PinVerifier` call in `ChangePin` in `withContext(Dispatchers.IO)`.
- [x] 2.4 Wrap the `PinVerifier` call in `IsCurrentPin` in `withContext(Dispatchers.IO)`.
- [x] 2.5 Wrap the `PinVerifier` call in `EnablePinLock` in `withContext(Dispatchers.IO)`.
- [x] 2.6 Confirm existing unit tests for these five use cases (using the fake `PinVerifier`) still pass unchanged.

## 3. Stop re-reading the stored language on configuration change (D3)

- [x] 3.1 Cache the language value read by `applyStoredLanguage()` in `startWhenUnlocked()` on a field in `PillsnerApplication`.
- [x] 3.2 Change `onConfigurationChanged()` to reapply the cached value via `AppLocale.apply(...)` directly, without reading `languageRepository` again.
- [x] 3.3 Add or update a test/manual check confirming a locale change still applies the correct language with no DataStore read on that path (this is `PillsnerApplication`, so a manual test case is acceptable if it isn't practically unit-testable — document it in this change's manual test notes).

## 4. Single-subscription theme StateFlow (D4)

- [x] 4.1 Rework `AppContainer.theme` so `themeRepository.observeTheme()` is collected exactly once, with the initial synchronous value and the hot `StateFlow` both derived from that single collection (see design.md D4).
- [x] 4.2 Manually verify cold start still shows the correct theme with no flash of the other scheme (light stored + phone in dark mode, and vice versa), since `AppContainer` isn't unit-testable in isolation from Android context.

## 5. Verification

- [x] 5.1 Run the full unit test suite (`./gradlew testDebugUnitTest` from `src/`) and confirm it passes.
- [x] 5.2 Run lint (`./gradlew lintDebug` from `src/`) and confirm it is clean.
- [x] 5.3 Run the `applock` instrumented tests (`./gradlew connectedDebugAndroidTest` or the relevant module task) since Keystore-backed PIN verification changed.
- [x] 5.4 Manually verify: PIN unlock, PIN change, and biometric fallback all still work correctly end to end on a device/emulator.
