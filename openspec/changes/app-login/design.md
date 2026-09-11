## Context

Pillsner is a single-activity Kotlin app using Jetpack Compose, Material 3, MVVM with unidirectional data flow, and strict on-device data. The `src/` folder is empty at the time of writing: no Gradle project, no activity, no navigation, no settings screen. This change is therefore likely to be the first one applied, and it must both introduce the app lock and lay down the smallest scaffold that can host it.

The lock protects the UI, not the storage. It is aimed at the casual threat: a family member, colleague or stranger holding an unlocked phone. It is not aimed at a forensic adversary with root access, which is why database encryption is out of scope.

Relevant constraints from `CLAUDE.md`:
- Kotlin only, Compose only, single activity, MVVM with UI state flowing down and events flowing up.
- Domain layer with no Android framework dependencies so it can be unit tested.
- First-party AndroidX libraries only, versions in a single version catalog.
- A single DI mechanism agreed in a proposal. None has been agreed yet.
- No network, no telemetry, and never log sensitive data.

## Goals / Non-Goals

**Goals:**
- Opt-in PIN lock with a setup-and-confirm flow, enforced on cold start and on every return to the foreground.
- Optional biometric unlock that bypasses the PIN, with the PIN always available as fallback.
- PIN never recoverable from what is persisted, and lock settings never leave the device.
- Guessing is slowed by a persistent, escalating cooldown.
- No permanent lockout of the user's own data when the device-bound key disappears.
- A gate at the root of the UI so all future screens are protected automatically.
- The minimum project scaffold needed to host and test the above, created only if `src/` is still empty.

**Non-Goals:**
- Change-PIN flow, grace period, database encryption, remote reset (see proposal).
- Deciding whether notification actions require unlocking (belongs to the reminders change).
- A complete settings screen. Only a Settings screen with a Security section is created; other sections come with their own changes.

## Decisions

### D1. Lock gate lives at the root of the single activity

The activity's content is a single `PillsnerApp` composable. It observes `AppLockUiState` from an `AppLockViewModel` scoped to the activity. When the state is `Locked`, `Recovering` or `Loading`, it renders the unlock screen (or a blank surface while loading) and does **not** compose the `NavHost`. When the state is `Unlocked` or `Disabled`, it composes the `NavHost`. The `NavController` and its back stack are created by `rememberNavController()` at the root, above the branch, so navigation state survives a relock and the user lands where they were.

*Alternatives considered:* (a) a lock destination inside the `NavHost`. Rejected because content composables would still be in the back stack and could be reached with a back press or briefly rendered during transitions. (b) A separate lock activity. Rejected because the project is single-activity by convention and an extra activity complicates recents and foreground detection.

### D2. Foreground and background detection via `ProcessLifecycleOwner`

`androidx.lifecycle:lifecycle-process` gives an app-wide lifecycle. `ON_STOP` fires only when no activity of the app is started, which is exactly "the app went to the background". `ON_PAUSE` is deliberately not used because system dialogs (biometric prompt, permission dialogs) pause the activity without the app leaving the foreground. A `LockOnBackgroundObserver` registered from the `Application` class sets the domain `LockState` to locked on `ON_STOP` when the lock is enabled. Cold start is handled by initialising the state as locked whenever the persisted enabled flag is true.

There is no grace period. The duration is still modelled as a single `LockPolicy.gracePeriod` constant set to zero so a later change can revisit it without restructuring.

### D3. PIN verifier is an HMAC with an Android Keystore key

A 4 to 6 digit PIN has at most one million values. Any hash that can be evaluated off the device, including PBKDF2 with a high iteration count, is brute-forced in minutes once the preferences file is copied. Instead the verifier is `HMAC-SHA256(key, salt || pin)` where `key` is a 256-bit HMAC key generated in `AndroidKeyStore` with `KeyGenParameterSpec` for `PURPOSE_SIGN`, not exportable and not user-authentication bound. The salt is 16 random bytes from `SecureRandom`. Verification recomputes the HMAC on device and compares in constant time (`MessageDigest.isEqual`). Without the Keystore key the persisted verifier is useless, and the key never leaves the device's secure hardware or TEE.

The domain layer defines `PinVerifier` (interface: `create(pin): PinCredential`, `verify(pin, credential): Boolean`, `isAvailable(): Boolean`) with no Android types. The data layer provides `KeystorePinVerifier`. Tests use an in-memory `FakePinVerifier`.

*Alternatives considered:* PBKDF2 or Argon2 in plain storage (weak offline, see above). `EncryptedSharedPreferences` from `androidx.security.crypto` (deprecated, and still leaves the hash brute-forceable once decrypted). Binding the Keystore key to user authentication (would make PIN verification depend on device biometrics, breaking the PIN-only path).

### D4. Lock settings in a dedicated Preferences DataStore, excluded from backup

Lock settings are a handful of scalars: enabled flag, biometric flag, salt, verifier, consecutive failure count, cooldown end time (epoch millis), and a settings version. They live in their own `applock` Preferences DataStore file so the backup exclusion rule can target exactly that file. `dataExtractionRules` (API 31+) and `fullBackupContent` (older) both exclude it. Room is not used because these are not domain records and a table would drag a schema migration into every tweak.

Restoring a backup on a new device therefore yields an app with the lock disabled rather than an unverifiable lock. This is the intended behaviour for a device-local secret.

### D5. Recovery when the Keystore key is gone

The key can vanish outside our control (factory reset with restored data, some OEM Keystore bugs, or the settings file restored by a mechanism that ignores backup rules). On start-up `KeystorePinVerifier.isAvailable()` checks for the key alias. If the lock is enabled and the key is missing, the state becomes `Recovering`. The unlock screen then offers "Reset app lock", which shows `BiometricPrompt` with `Authenticators.DEVICE_CREDENTIAL` combined with `BIOMETRIC_WEAK`. Success clears all lock settings and moves to `Disabled`. This keeps the user's data reachable to the person who can unlock the device, which is the same trust boundary the lock was defending in the first place.

### D6. Biometric class and prompt behaviour

`BiometricManager.canAuthenticate(BIOMETRIC_WEAK)` decides availability. Class 2 (weak) is accepted because face unlock on many mid-range devices is class 2, and the prompt gates UI access rather than a cryptographic key, so the stronger class buys little. No `CryptoObject` is used. The prompt's negative button is "Use PIN". On the unlock screen the prompt is presented automatically once per lock episode; after cancel, `ERROR_LOCKOUT` or `ERROR_LOCKOUT_PERMANENT` it is not re-presented automatically and a "Use biometrics" button lets the user retry. Enabling the biometric preference requires one successful prompt so the user proves the enrolled biometric is theirs and works.

When `canAuthenticate` stops returning success while the preference is on, `AppLockRepository.reconcileBiometricAvailability()` turns the preference off and the UI explains why. This runs on every unlock screen appearance and every Security section appearance.

`BiometricPrompt` needs a `FragmentActivity`, so `MainActivity` extends `FragmentActivity` (which `ComponentActivity`-based Compose setup supports). The prompt is driven from the activity through a small `BiometricAuthenticator` wrapper that exposes a suspend function returning a sealed result (`Success`, `Cancelled`, `LockedOut`, `Unavailable`, `Failed`), keeping the view model free of Android prompt callbacks.

### D7. Failed-attempt cooldown is domain logic

`LockPolicy` is a pure Kotlin object: `maxAttemptsPerWindow = 5`, `baseCooldown = 30s`, `maxCooldown = 5min`, `cooldownFor(consecutiveFailures)` returns the cooldown for the block just completed (30s, 60s, 120s, 240s, 300s cap). `AppLockRepository` persists the failure count and the cooldown end instant. The unlock screen derives remaining time from a `Clock` abstraction injected into the view model so tests can advance time. The same repository methods serve both the unlock screen and the disable-lock dialog so the counter is shared.

### D8. Recents thumbnail via `FLAG_SECURE`

While the enabled flag is true the activity sets `WindowManager.LayoutParams.FLAG_SECURE`; when it becomes false the flag is cleared. The flag is toggled from the activity by collecting the enabled flag, not from composables, because it is a window property. Trade-off: the flag also blocks user screenshots and screen recording of the app while the lock is on. This is accepted for a health data app and stated in the Security section's helper text.

*Alternative considered:* setting `Locked` on `ON_PAUSE` and hoping recomposition beats the thumbnail capture. Unreliable and it reintroduces the dialog problem from D2.

### D9. Dependency injection: manual `AppContainer`

No DI mechanism exists yet and this change needs one. The proposal chooses manual constructor injection through a single `AppContainer` interface held by the `Application` class, with a `DefaultAppContainer` for release and test doubles in tests. View models are created through a small `viewModelFactory` that pulls from the container. Rationale: the app is small, this avoids annotation processing and build-time cost, and it keeps constructors honest. Should the app outgrow this, a later proposal can introduce Hilt and the constructors already fit.

### D10. Minimal project scaffold (conditional)

If `src/` holds no Gradle project when the change is applied, the first task creates one: Gradle Kotlin DSL, a single `app` module, `libs.versions.toml` version catalog, application id `nl.hexmaster.pillsner`, `compileSdk` at the current stable SDK, `minSdk` 26, Compose BOM, Material 3, Navigation Compose, `lifecycle-viewmodel-compose`, `lifecycle-process`, `datastore-preferences`, `biometric`, plus JUnit and Compose UI test dependencies. Screens: `Home` (placeholder text with a link to Settings) and `Settings` (hosting the Security section). Nothing else. If a scaffold already exists, this task is skipped and the packages below are placed into it.

### Package layout

```
nl.hexmaster.pillsner
  PillsnerApplication            // holds AppContainer, registers LockOnBackgroundObserver
  MainActivity                   // sets content, toggles FLAG_SECURE, hosts BiometricAuthenticator
  ui/PillsnerApp.kt              // root gate (D1) + NavHost
  ui/home/, ui/settings/         // scaffold screens
  di/AppContainer.kt, di/DefaultAppContainer.kt
  applock/
    domain/  LockState, LockPolicy, PinCredential, PinVerifier, AppLockRepository (interface),
             use cases: EnablePinLock, DisablePinLock, UnlockWithPin, RegisterFailedAttempt,
             SetBiometricUnlock, ResetLockAfterRecovery
    data/    DataStoreAppLockRepository, KeystorePinVerifier, BiometricAvailability (wrapper)
    ui/      AppLockViewModel, UnlockScreen, PinSetupScreen, SecuritySection, PinKeypad,
             BiometricAuthenticator
```

### State machine

```
Disabled --enable(pin)--> Unlocked(enabled)
Unlocked --ON_STOP--> Locked
Locked --correct pin / biometric ok--> Unlocked
Locked --wrong pin x5--> Locked(coolingDown until t)
Locked --key missing--> Recovering
Recovering --device credential ok--> Disabled
Unlocked --disable(correct pin)--> Disabled
cold start: enabled ? (key present ? Locked : Recovering) : Disabled
```

## Risks / Trade-offs

- [Relocking on every background switch is irritating] → Requested explicitly; kept to a single constant (D2) so a grace period is a one-line follow-up change. Biometric unlock makes the common case one glance.
- [`FLAG_SECURE` blocks screenshots] → Stated in the Security section helper text; only applies while the lock is on.
- [Keystore key lost, user locked out] → Recovery path via device credential (D5); lock settings excluded from backup so restores never produce the mismatch (D4).
- [Class 2 biometrics can be spoofed more easily than class 3] → Accepted; the lock defends against casual access and the PIN remains the anchor. Documented in D6.
- [`ProcessLifecycleOwner.ON_STOP` fires a little after the app is hidden] → Content is hidden by `FLAG_SECURE` in recents anyway; the lock is enforced before the user can interact again.
- [Cooldown can be bypassed by clearing app data] → Clearing app data also removes the medication data, so there is nothing left to protect.
- [Biometric prompt on some devices reports `ERROR_HW_UNAVAILABLE` transiently] → Treated like cancel: fall back to PIN, offer retry, do not turn the preference off unless `canAuthenticate` reports no hardware or none enrolled.
- [Scaffold decisions (DI, minSdk, application id) are made inside a feature change] → Called out in the proposal impact and here; reviewers can object before apply. They are the smallest choices that unblock the work.

## Migration Plan

Greenfield: no existing users, no data migration. The lock settings DataStore carries a `settingsVersion` key from day one so a later change can migrate it. Rollback is removing the feature area; the scaffold stays.

## Open Questions

- Should notification actions (confirm, snooze, skip) work while the app is locked? Deferred to the reminders change; the gate in D1 does not affect notification actions because they are handled outside the activity.
- Application id `nl.hexmaster.pillsner` and `minSdk` 26 are proposed defaults. Confirm before publishing a first build, since the application id cannot change afterwards.
