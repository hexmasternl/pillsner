## 1. Preconditions

- [ ] 1.1 Verify `app-login` is applied: `AppLockRepository`, `KeystorePinVerifier`, `LockPolicy`, `RegisterFailedAttempt`, `BiometricAuthenticator`, `PinKeypad`, `PinSetupScreen`, `SecuritySection` and the disable confirm dialog all exist; stop and report if not
- [ ] 1.2 Note how the Settings screen is composed (from `app-login` or `app-settings-language`) so the Security section is extended in place rather than recreated

## 2. Domain

- [ ] 2.1 Add `SecurityAction`, `IdentityMethod`, `VerifyIdentityRequest` and `VerifyIdentityState` (Idle, AwaitingBiometric, AwaitingPin with cooldown end, Verified) to `applock/domain`
- [ ] 2.2 Implement the `VerifyIdentity` use case: initial state selection (biometric first only when allowed, enabled and available), biometric result handling that falls back to PIN without touching the preference, PIN submission via the existing verifier and `RegisterFailedAttempt`, reset on success, and single consumption of `Verified`
- [ ] 2.3 Add `replaceCredential(credential)` to `AppLockRepository`: one atomic write replacing salt and verifier and resetting the failure count and cooldown, leaving enabled and biometric flags untouched
- [ ] 2.4 Implement the `ChangePin` use case returning `SameAsCurrent` when the new PIN verifies against the current credential, otherwise creating a new credential and calling `replaceCredential`
- [ ] 2.5 Unit tests for `VerifyIdentity`: biometric first when eligible, PIN only when biometrics disabled or refused, fallback on cancel, failure and lockout, wrong PIN increments the shared count, cooldown refuses entry, `Verified` consumed once, dismiss returns to Idle
- [ ] 2.6 Unit tests for `ChangePin`: same PIN rejected, new credential replaces the old, old PIN no longer verifies, biometric flag preserved, failure count and cooldown reset

## 3. Data

- [ ] 3.1 Implement `replaceCredential` in `DataStoreAppLockRepository` as a single `edit` and add it to the fake repository used by tests
- [ ] 3.2 Instrumented test: after `replaceCredential`, the stored salt and verifier differ, enabled and biometric flags are unchanged and the failure count is zero

## 4. Identity check UI

- [ ] 4.1 Build `VerifyIdentityDialog`: purpose-specific title, `AwaitingBiometric` content with "Use PIN", `AwaitingPin` content with `PinKeypad`, live-region error, cooldown countdown that disables the keypad, optional "Use biometrics" control, and a Cancel action; scrollable content for large fonts
- [ ] 4.2 Launch the biometric prompt through `BiometricAuthenticator` exactly once on entering `AwaitingBiometric`, and route its result to the view model
- [ ] 4.3 Extend `SecurityViewModel` (or create it if the section had no dedicated view model) with `verify` state, the events from the design, dispatch of `Verified` by purpose, a one-shot navigation effect for `CHANGE_PIN`, one-shot messages, and reset of `verify` to Idle when `LockState` becomes Locked
- [ ] 4.4 Replace `app-login`'s disable confirm dialog with `VerifyIdentityDialog` configured with biometrics refused; keep its behaviour and move its tests
- [ ] 4.5 Add strings for the three dialog titles, "Use PIN", "Use biometrics", "Confirm it's you" helper text and the Cancel action

## 5. Security section and Change PIN

- [ ] 5.1 Add the "Change PIN" row to `SecuritySection` between the two switches, visible only when the lock is enabled, with button semantics and a chevron
- [ ] 5.2 Wire "Change PIN" → identity check (`CHANGE_PIN`, biometrics allowed) → navigation to `PinSetup(CHANGE)`
- [ ] 5.3 Wire turning "Unlock with biometrics" off → identity check (`DISABLE_BIOMETRICS`, biometrics allowed) → `SetBiometricUnlock(false)`; keep the switch bound to persisted state so it does not move until saved
- [ ] 5.4 Add `PinSetupMode` (SET_UP, CHANGE) as a route argument to `PinSetupScreen`; in CHANGE mode use the new titles, reject a first-step PIN that verifies against the current credential, and call `ChangePin` on confirmation
- [ ] 5.5 On successful change, pop back to the Security section and show the "PIN changed" confirmation; ensure back or background before confirmation leaves the current PIN valid
- [ ] 5.6 Add strings for the Change PIN row, the change-mode titles, the same-PIN message and the confirmation message
- [ ] 5.7 Unit tests for `SecurityViewModel`: Change PIN requires a check, biometrics off requires a check, biometrics on does not, disable refuses biometrics, `Verified` dispatches once, relock resets the check
- [ ] 5.8 Compose tests: Change PIN row hidden when the lock is off and visible when on; full change-PIN flow with PIN identification; same-PIN rejection; mismatch restart; biometrics-off check cancelled leaves the switch on; disable dialog shows no biometric option; identity check and keypad at 200 percent font scale

## 6. Verification and documentation

- [ ] 6.1 Run `./gradlew test` and `./gradlew lint` from `src/`; fix failures and report results verbatim
- [ ] 6.2 Run `./gradlew connectedAndroidTest` for the DataStore and Keystore tests
- [ ] 6.3 Manual test on a device with a biometric enrolled: change PIN via biometric identification, change PIN via PIN identification, old PIN rejected on the unlock screen, turn biometrics off via biometric and via PIN, disable lock shows PIN only, background during the check and during change PIN leaves settings unchanged, TalkBack announces the Change PIN row and check errors
- [ ] 6.4 Update `README.md`: the app lock line mentions changing the PIN and re-identification for lock changes
- [ ] 6.5 Review against `CLAUDE.md`: no Android imports in `applock/domain`, strings in resources, no PIN or credential logged, no new dependencies
- [ ] 6.6 Confirm archive order: `app-login` before this change
