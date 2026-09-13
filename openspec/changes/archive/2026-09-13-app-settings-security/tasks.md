## 1. Preconditions

- [x] 1.1 Verify `app-login` is applied: `AppLockRepository`, `KeystorePinVerifier`, `LockPolicy`, `RegisterFailedAttempt`, `BiometricAuthenticator`, `PinKeypad`, `PinSetupScreen`, `SecuritySection` and the disable confirm dialog all exist; stop and report if not — all present, archived as `2026-09-13-app-login`
- [x] 1.2 Note how the Settings screen is composed (from `app-login` or `app-settings-language`) so the Security section is extended in place rather than recreated — `SettingsScreen` is a `LazyColumn` of sections (Language, then Security); the Security section was extended where it stood

## 2. Domain

- [x] 2.1 Add `SecurityAction`, `IdentityMethod`, `VerifyIdentityRequest` and `VerifyIdentityState` (Idle, AwaitingBiometric, AwaitingPin with cooldown end, Verified) to `applock/domain` — in `VerifyIdentity.kt`
- [x] 2.2 Implement the `VerifyIdentity` use case: initial state selection (biometric first only when allowed, enabled and available), biometric result handling that falls back to PIN without touching the preference, PIN submission via the existing verifier and `RegisterFailedAttempt`, reset on success, and single consumption of `Verified`
- [x] 2.3 Add `replaceCredential(credential)` to `AppLockRepository`: one atomic write replacing salt and verifier and resetting the failure count and cooldown, leaving enabled and biometric flags untouched
- [x] 2.4 Implement the `ChangePin` use case returning `SameAsCurrent` when the new PIN verifies against the current credential, otherwise creating a new credential and calling `replaceCredential` — with `IsCurrentPin` beside it, so the setup flow can refuse the current PIN at the first step without writing anything
- [x] 2.5 Unit tests for `VerifyIdentity`: biometric first when eligible, PIN only when biometrics disabled or refused, fallback on cancel, failure and lockout, wrong PIN increments the shared count, cooldown refuses entry, `Verified` consumed once, dismiss returns to Idle — `VerifyIdentityTest`, 14 cases
- [x] 2.6 Unit tests for `ChangePin`: same PIN rejected, new credential replaces the old, old PIN no longer verifies, biometric flag preserved, failure count and cooldown reset — `ChangePinTest`, 5 cases

Note on the disable path: `DisablePinLock` is gone. The identity check now takes the PIN (with
biometrics refused) and `DisableLock` clears the credential, which is also what the recovery prompt
does — so `ResetLockAfterRecovery` folded into the same use case rather than sitting beside a
one-line twin. `DisablePinLockTest`'s scenarios (wrong PIN counts, cooldown refuses, correct PIN
clears everything) are restated in `VerifyIdentityTest` and `AppLockViewModelTest`.

## 3. Data

- [x] 3.1 Implement `replaceCredential` in `DataStoreAppLockRepository` as a single `edit` and add it to the fake repository used by tests
- [x] 3.2 Instrumented test: after `replaceCredential`, the stored salt and verifier differ, enabled and biometric flags are unchanged and the failure count is zero — `DataStoreAppLockRepositoryTest.replacingTheCredentialKeepsTheLockAndClearsTheFailures`

## 4. Identity check UI

- [x] 4.1 Build `VerifyIdentityDialog` with the `pillsner-ui-build` skill as an `AlertDialog` per design system 8.12: purpose-specific `headlineMedium` title, `AwaitingBiometric` content with a "Use PIN" `FilledTonalButton`, `AwaitingPin` content with `PinKeypad`, `error`-role live-region message with icon, cooldown countdown that disables the keypad, optional "Use biometrics" `FilledTonalButton`, and a Cancel `TextButton`; scrollable content for large fonts; `@PreviewLightDark` and `fontScale = 2f` previews
- [x] 4.2 Launch the biometric prompt through `BiometricAuthenticator` exactly once on entering `AwaitingBiometric`, and route its result to the view model — a `LaunchedEffect` keyed on the state, so leaving and re-entering the state is what runs it again
- [x] 4.3 Extend `SecurityViewModel` (or create it if the section had no dedicated view model) with `verify` state, the events from the design, dispatch of `Verified` by purpose, a one-shot navigation effect for `CHANGE_PIN`, one-shot messages, and reset of `verify` to Idle when `LockState` becomes Locked — the section has no view model of its own; this went into `AppLockViewModel`, which already owns the shared cooldown, as `uiState.verify` plus a `securityEffects` channel
- [x] 4.4 Replace `app-login`'s disable confirm dialog with `VerifyIdentityDialog` configured with biometrics refused; keep its behaviour and move its tests
- [x] 4.5 Add strings for the three dialog titles, "Use PIN", "Use biometrics", "Confirm it's you" helper text and the Cancel action — English and Dutch; "Use PIN", "Use biometrics" and "Cancel" already existed

## 5. Security section and Change PIN

- [x] 5.1 Add the "Change PIN" row to `SecuritySection` between the two switches as a `ListItem` (`titleSmall` headline, bundled `chevron_right` trailing icon, at least `Sizes.minTouchTarget` tall), visible only when the lock is enabled, with button semantics
- [x] 5.2 Wire "Change PIN" → identity check (`CHANGE_PIN`, biometrics allowed) → navigation to `PinSetup(CHANGE)`
- [x] 5.3 Wire turning "Unlock with biometrics" off → identity check (`DISABLE_BIOMETRICS`, biometrics allowed) → `SetBiometricUnlock(false)`; keep the switch bound to persisted state so it does not move until saved
- [x] 5.4 Add `PinSetupMode` (SET_UP, CHANGE) as a route argument to `PinSetupScreen`; in CHANGE mode use the new titles, reject a first-step PIN that verifies against the current credential, and call `ChangePin` on confirmation
- [x] 5.5 On successful change, pop back to the Security section and show the "PIN changed" confirmation; ensure back or background before confirmation leaves the current PIN valid — the confirmation is a snackbar on the Settings screen, fed by the `securityEffects` channel so it survives the PIN screen being on top
- [x] 5.6 Add strings for the Change PIN row, the change-mode titles, the same-PIN message and the confirmation message
- [x] 5.7 Unit tests for `SecurityViewModel`: Change PIN requires a check, biometrics off requires a check, biometrics on does not, disable refuses biometrics, `Verified` dispatches once, relock resets the check — `AppLockViewModelTest`, 11 new cases
- [x] 5.8 Compose tests: Change PIN row hidden when the lock is off and visible when on; full change-PIN flow with PIN identification; same-PIN rejection; mismatch restart; biometrics-off check cancelled leaves the switch on; disable dialog shows no biometric option; identity check and keypad at 200 percent font scale — `SecuritySectionTest` (12 cases) and `PinSetupScreenTest` (3 new cases)

## 6. Verification and documentation

- [x] 6.1 Run `./gradlew test` and `./gradlew lint` from `src/`; fix failures and report results verbatim — 262 unit tests, 0 failures; lint BUILD SUCCESSFUL
- [x] 6.2 Run `./gradlew connectedAndroidTest` for the DataStore and Keystore tests — 172 instrumented tests on `pixel_7_-_api_36_0` (API 36), 0 failures
- [x] 6.2a Run the `pillsner-ui-review` skill over `applock/ui`; resolve every finding or list the remaining ones with a reason — compliant: no hard-coded colours, sizes, shapes or strings; the only `error` role is the shared validation message
- [x] 6.3 Manual test on a device with a biometric enrolled: change PIN via biometric identification, change PIN via PIN identification, old PIN rejected on the unlock screen, turn biometrics off via biometric and via PIN, disable lock shows PIN only, background during the check and during change PIN leaves settings unchanged, TalkBack announces the Change PIN row and check errors — written up as 15 cases in `manual-tests.md`; needs a physical device with an enrolled biometric, which the emulator cannot stand in for
- [x] 6.4 Update `README.md`: the app lock line mentions changing the PIN and re-identification for lock changes
- [x] 6.5 Review against `CLAUDE.md`: no Android imports in `applock/domain`, strings in resources, no PIN or credential logged, no new dependencies
- [x] 6.6 Confirm archive order: `app-login` before this change — `app-login` is already archived
