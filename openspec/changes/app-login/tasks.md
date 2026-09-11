## 1. Project scaffold (skip any task whose result already exists in `src/`)

- [ ] 1.1 Create the Gradle Kotlin DSL project in `src/` with a single `app` module, `settings.gradle.kts`, `gradle/libs.versions.toml` version catalog, Gradle wrapper, and a `.gitignore` covering build output, IDE metadata and `local.properties`
- [ ] 1.2 Configure the `app` module: application id `nl.hexmaster.pillsner`, `minSdk` 26, `compileSdk` and `targetSdk` at the current stable SDK, Compose enabled, Kotlin JVM toolchain set
- [ ] 1.3 Add catalog entries and dependencies for Compose BOM, Material 3, Activity Compose, Navigation Compose, `lifecycle-viewmodel-compose`, `lifecycle-process`, `datastore-preferences`, `biometric`, `core-ktx`, JUnit 4, `kotlinx-coroutines-test`, Compose UI test and AndroidX test runner
- [ ] 1.4 Create `PillsnerApplication` holding an `AppContainer`, register it in the manifest, and create `MainActivity` (extending `FragmentActivity`) that sets Compose content to `PillsnerApp`
- [ ] 1.5 Create `di/AppContainer.kt` (interface) and `di/DefaultAppContainer.kt` plus a `viewModelFactory` helper that resolves view models from the container
- [ ] 1.6 Create `ui/PillsnerApp.kt` with a Material 3 theme, `rememberNavController()` and a `NavHost` with `Home` (placeholder text and a Settings button) and `Settings` (empty list ready for sections) destinations
- [ ] 1.7 Add `res/values/strings.xml` with the scaffold strings, and confirm `assembleDebug` and `test` run green from `src/`

## 2. App lock domain layer (pure Kotlin, no Android dependencies)

- [ ] 2.1 Define `LockState` (sealed: `Loading`, `Disabled`, `Unlocked`, `Locked` with optional cooldown end, `Recovering`) and `PinCredential` (salt and verifier as byte arrays)
- [ ] 2.2 Define `PinVerifier` interface (`isAvailable()`, `create(pin)`, `verify(pin, credential)`) and a `Pin` value class that validates 4 to 6 decimal digits
- [ ] 2.3 Define `LockPolicy` with `maxAttemptsPerWindow = 5`, `baseCooldown = 30s`, `maxCooldown = 5min`, `gracePeriod = 0` and `cooldownFor(consecutiveFailures)` implementing the doubling schedule capped at 5 minutes
- [ ] 2.4 Define `AppLockRepository` interface exposing `settings: Flow<AppLockSettings>` and suspend functions to store credential, clear credential, set biometric flag, record failed attempt with cooldown end, reset attempts, and reconcile biometric availability
- [ ] 2.5 Define a `Clock` abstraction and a `BiometricAvailability` interface (`status(): Available | NoneEnrolled | NoHardware | Unavailable`)
- [ ] 2.6 Implement use cases `EnablePinLock`, `DisablePinLock`, `UnlockWithPin`, `RegisterFailedAttempt`, `SetBiometricUnlock`, `ResetLockAfterRecovery` and a `ResolveInitialLockState` use case (enabled and key present → Locked, enabled and key missing → Recovering, else Disabled)
- [ ] 2.7 Unit tests for `Pin` validation, `LockPolicy.cooldownFor` (30s, 60s, 120s, 240s, 300s cap), `UnlockWithPin` success and failure paths, cooldown persistence across a simulated restart, `DisablePinLock` refusing biometrics and honouring cooldown, and `ResolveInitialLockState` for all three branches, using `FakePinVerifier`, `FakeAppLockRepository` and a test clock

## 3. App lock data layer

- [ ] 3.1 Implement `KeystorePinVerifier`: generate or load a non-exportable HMAC-SHA256 key in `AndroidKeyStore` under a fixed alias, 16-byte `SecureRandom` salt, HMAC over `salt || pin`, constant-time comparison with `MessageDigest.isEqual`, `isAvailable()` checking the alias
- [ ] 3.2 Implement `DataStoreAppLockRepository` on a dedicated `applock` Preferences DataStore with keys for enabled, biometric enabled, salt, verifier, consecutive failures, cooldown end millis and settings version 1
- [ ] 3.3 Implement `AndroidBiometricAvailability` wrapping `BiometricManager.canAuthenticate(BIOMETRIC_WEAK)` and mapping results to the domain status
- [ ] 3.4 Add `dataExtractionRules` (API 31+) and `fullBackupContent` resources excluding the `applock` DataStore file from cloud backup and device transfer, and reference them from the manifest
- [ ] 3.5 Wire the three implementations into `DefaultAppContainer`
- [ ] 3.6 Instrumented tests: `KeystorePinVerifier` round trip (create then verify correct and wrong PIN), `isAvailable()` false after the alias is deleted, and `DataStoreAppLockRepository` persisting and clearing settings

## 4. Lock state and enforcement

- [ ] 4.1 Implement `AppLockViewModel` (activity scoped) exposing `AppLockUiState` derived from `LockState`, remaining cooldown from the injected clock, biometric availability, and one-shot events for showing the biometric prompt
- [ ] 4.2 Implement `LockOnBackgroundObserver` on `ProcessLifecycleOwner` that moves the state to `Locked` on `ON_STOP` when the lock is enabled, and register it from `PillsnerApplication`
- [ ] 4.3 Resolve the initial state on cold start via `ResolveInitialLockState` before any content is composed, showing a blank surface while `Loading`
- [ ] 4.4 Update `PillsnerApp` so the `NavHost` is composed only in `Unlocked` and `Disabled`; `Locked` and `Recovering` render `UnlockScreen`; the `NavController` is created above the branch so navigation state survives a relock
- [ ] 4.5 In `MainActivity`, collect the enabled flag and set or clear `FLAG_SECURE` on the window accordingly
- [ ] 4.6 Implement `BiometricAuthenticator` in the activity: a suspend wrapper over `BiometricPrompt` returning `Success`, `Cancelled`, `LockedOut`, `Unavailable` or `Failed`, supporting `BIOMETRIC_WEAK` with a "Use PIN" negative button and a `DEVICE_CREDENTIAL | BIOMETRIC_WEAK` variant for recovery
- [ ] 4.7 Unit tests for `AppLockViewModel`: locked on `ON_STOP` only when enabled, biometric prompt requested once per lock episode, no re-prompt after cancel or lockout, cooldown countdown from the test clock, recovery state when the verifier is unavailable

## 5. Unlock screen

- [ ] 5.1 Build `PinKeypad` composable: digits 0 to 9, backspace, submit, masked dots for 4 to 6 digits, keypad anchored to the lower part of the screen, minimum 48dp touch targets, content descriptions for TalkBack
- [ ] 5.2 Build `UnlockScreen` for `Locked`: keypad, wrong-PIN error with live-region announcement, cooldown countdown that disables the keypad, "Use biometrics" button when biometric unlock is enabled, automatic prompt on first appearance
- [ ] 5.3 Build the `Recovering` variant of `UnlockScreen`: explanation text and a "Reset app lock" action that runs the device-credential prompt and, on success, calls `ResetLockAfterRecovery`
- [ ] 5.4 Reconcile biometric availability when the unlock screen appears, hiding biometric controls and turning the preference off when biometrics are no longer enrolled or hardware is absent
- [ ] 5.5 Add all unlock screen strings to `strings.xml`
- [ ] 5.6 Compose semantics tests: correct PIN unlocks, wrong PIN clears entry and shows error, fifth failure disables keypad and shows countdown, recovery variant shows the reset action and hides the keypad

## 6. PIN setup and Security settings section

- [ ] 6.1 Build `PinSetupScreen` as a two-step flow (enter, confirm) using `PinKeypad`, rejecting fewer than 4 digits with a minimum-length hint, restarting at step one with a mismatch message when the confirmation differs, and storing nothing until both match
- [ ] 6.2 Ensure navigating back or backgrounding during setup leaves the lock disabled and clears any in-memory PIN
- [ ] 6.3 Build `SecuritySection` for the Settings screen: "Protect with PIN" switch and "Unlock with biometrics" switch, the latter disabled with an explanation when the PIN lock is off or when no biometric is enrolled or no hardware is present, plus helper text noting that screenshots are blocked while the lock is on
- [ ] 6.4 Turning "Protect with PIN" on navigates to `PinSetupScreen`; on success the switch shows on and the lock state becomes `Unlocked`
- [ ] 6.5 Turning "Protect with PIN" off opens a confirm-PIN dialog using `PinKeypad`; correct PIN calls `DisablePinLock` and clears everything, wrong PIN shows the error and increments the shared failure count, active cooldown refuses entry and shows remaining time; biometrics are never offered here
- [ ] 6.6 Turning "Unlock with biometrics" on runs the biometric prompt and enables the preference only on success; cancel or failure leaves it off
- [ ] 6.7 Reconcile biometric availability when the Security section appears
- [ ] 6.8 Add all setup and Security section strings to `strings.xml`
- [ ] 6.9 Compose semantics tests: setup happy path, mismatch restart, too-short rejection, disable with correct and wrong PIN, biometric switch disabled when PIN lock is off

## 7. Verification and documentation

- [ ] 7.1 Run `test` and `lint` from `src/` and fix any findings
- [ ] 7.2 Run `connectedAndroidTest` on a device or emulator for the Keystore and DataStore tests
- [ ] 7.3 Manual test on a physical device with a biometric enrolled: cold start lock, relock after switching apps, relock after screen off, biometric bypass, "Use PIN" fallback, recents thumbnail hidden while enabled and visible when disabled, 200 percent font scale, TalkBack announcement of the wrong-PIN message
- [ ] 7.4 Manual test of the recovery path: enable the lock, delete the Keystore alias via a debug-only hook or `adb` on a debug build, restart, confirm the reset action works and the lock ends up disabled
- [ ] 7.5 Update `README.md`: add the app lock to the Privacy by default feature list, note that screenshots are blocked while the lock is on, and record the DI choice and any new table rows in the Technology section
