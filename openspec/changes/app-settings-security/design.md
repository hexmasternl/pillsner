## Context

`app-login` defines the lock: a root gate in `PillsnerApp`, `AppLockViewModel`, `AppLockRepository` on the `applock` DataStore, `KeystorePinVerifier`, `LockPolicy` with the shared cooldown, `BiometricAuthenticator` as a suspend wrapper over `BiometricPrompt`, `PinKeypad`, `PinSetupScreen` (two-step), a `SecuritySection` with two switches, and a confirm-PIN dialog used only to disable the lock. Biometrics are turned on with one prompt and off with a plain toggle. There is no change-PIN flow.

This change adds Change PIN and a re-identification step for PIN and biometric changes. The threat is the same casual one `app-login` defends against: someone holding the phone while Pillsner is unlocked. The check proves the person changing the lock is the one who can open it.

Constraints from `CLAUDE.md`: domain logic without Android dependencies, Compose with Material 3, strings in resources, accessibility for the keypad and messages, no new dependencies without a reason, never log PINs or credentials.

## Goals / Non-Goals

**Goals:**
- Change PIN in place, atomically, without disabling the lock or losing the biometric preference.
- One reusable identity check that accepts biometric or PIN, or PIN only, and shares the failed-attempt cooldown.
- Every sensitive Security change (change PIN, biometrics off, disable lock) goes through that check; biometrics on keeps its single prompt.
- Nothing changes if a flow is interrupted by a relock.

**Non-Goals:**
- Session-style re-authentication, changing the disable rule, recovery changes, PIN strength rules.

## Decisions

### D1. Identity check as a domain state machine, one action per verification

```kotlin
enum class IdentityMethod { BIOMETRIC, PIN }

data class VerifyIdentityRequest(val purpose: SecurityAction, val allowBiometric: Boolean)

enum class SecurityAction { CHANGE_PIN, DISABLE_BIOMETRICS, DISABLE_LOCK }

sealed interface VerifyIdentityState {
    data object Idle : VerifyIdentityState
    data class AwaitingBiometric(val request: VerifyIdentityRequest) : VerifyIdentityState
    data class AwaitingPin(val request: VerifyIdentityRequest, val cooldownEndsAt: Instant?) : VerifyIdentityState
    data class Verified(val request: VerifyIdentityRequest, val method: IdentityMethod) : VerifyIdentityState
}
```

`VerifyIdentity` (domain use case) starts in `AwaitingBiometric` when `allowBiometric` is true, biometric unlock is enabled and `BiometricAvailability` reports available; otherwise `AwaitingPin`. Biometric success → `Verified`. Biometric cancel, failure, lockout or unavailability → `AwaitingPin` (never turns the preference off; that stays the job of `reconcileBiometricAvailability`). A wrong PIN calls the existing `RegisterFailedAttempt`, which persists the count and cooldown; a correct PIN resets them and yields `Verified`. `Verified` is consumed exactly once by the requesting action and then returns to `Idle`. There is no timestamp and no reuse window.

*Why one action per verification:* it matches the request literally, is trivially testable, and keeps the security model the same as the unlock screen's: prove identity, do one thing. A reuse window would need a clock, an expiry policy and a story for what happens on relock.

*Why the check does not turn the biometric preference off on failure:* a failed prompt is not evidence that biometrics are gone; the unlock and Security screens already reconcile availability on appearance.

### D2. Change PIN reuses the setup flow in a "change" mode

`PinSetupScreen` gets a `PinSetupMode { SET_UP, CHANGE }` route argument. Both modes run the same two steps (enter, confirm), the same 4 to 6 digit rule and the same mismatch restart. `CHANGE` differs in three ways: the titles read "Choose a new PIN" and "Confirm your new PIN"; a PIN that verifies against the current credential is rejected with "Choose a PIN that differs from your current one" at the first step; on success it calls `ChangePin` instead of `EnablePinLock`.

```kotlin
class ChangePin(repository: AppLockRepository, verifier: PinVerifier) {
    suspend operator fun invoke(newPin: Pin): ChangePinResult   // SameAsCurrent | Changed
}
```

`ChangePin` creates a new credential (new salt, new HMAC) and calls `repository.replaceCredential(credential)`, a new repository method that writes salt and verifier in one DataStore `edit` and resets the failure count and cooldown in the same edit, leaving `enabled` and `biometricEnabled` untouched. The Keystore key is reused; only salt and verifier change.

*Why reject the same PIN:* it is what users expect from "change" and it is cheap to check on device. *Why not check at the confirmation step:* the user should learn early, before typing it twice.

*Alternative considered:* a dedicated Change PIN screen with three fields (current, new, confirm). Rejected: the current-PIN step is exactly the identity check, which may be biometric, and the keypad flow is already built and accessible.

### D3. Security section layout and flows

Rows, in order, inside the existing `SecuritySection`:

1. **Protect with PIN** switch (existing).
2. **Change PIN** row with a chevron, shown only when the lock is enabled. Tap → identity check (`CHANGE_PIN`, biometrics allowed) → on `Verified`, navigate to `PinSetup(CHANGE)` → on success, snackbar "PIN changed".
3. **Unlock with biometrics** switch (existing). Turning **on** → existing single biometric prompt, unchanged. Turning **off** → identity check (`DISABLE_BIOMETRICS`, biometrics allowed) → on `Verified`, `SetBiometricUnlock(false)`. The switch does not move until verification succeeds.
4. Helper text (existing).

Turning **Protect with PIN off** → identity check (`DISABLE_LOCK`, biometrics refused) → on `Verified`, `DisablePinLock`. This replaces `app-login`'s confirm-PIN dialog with the shared component configured for PIN only; behaviour is otherwise identical, including the cooldown.

The switches are controlled components bound to persisted state, so a pending or cancelled check never leaves a switch showing a value that was not saved.

### D4. `VerifyIdentityDialog` composable

A Material 3 `AlertDialog` (full-width on phones) titled by purpose: "Confirm it's you to change your PIN", "... to turn off biometric unlock", "... to turn off the app lock". Content by state:

- `AwaitingBiometric`: a short line "Use your fingerprint or face" and a "Use PIN" button; the biometric prompt is launched once on entering the state via `BiometricAuthenticator` from the activity, the same wrapper the unlock screen uses.
- `AwaitingPin`: `PinKeypad` with masked entry, wrong-PIN error in a polite live region, cooldown countdown that disables the keypad and shows remaining time (from the injected `Clock`), and, when biometrics are allowed and available, a "Use biometrics" button to go back to `AwaitingBiometric`.
- Dismiss (back, outside tap, Cancel) → `Idle`, nothing changes.

The dialog owns no logic: it renders `VerifyIdentityState` and sends events to `SecurityViewModel`.

*Why a dialog rather than a destination:* it keeps the user on the Security section, matches `app-login`'s existing disable dialog, and avoids adding back-stack entries that could be revisited after the check is consumed.

### D5. Abandonment on relock

All these flows live under the root gate from `app-login` D1. When the app goes to the background the state becomes `Locked`, the gate stops composing the `NavHost`, and `SecurityViewModel`'s in-flight `VerifyIdentityState` is reset to `Idle` by observing `LockState`. `PinSetupScreen` already clears its in-memory PIN on background (`app-login` task 6.2); in `CHANGE` mode that means the old credential stays. No new mechanism is needed, only a test that proves it.

### D6. Interaction with the biometric-on prompt

Turning biometrics on is deliberately not routed through `VerifyIdentity`: its existing prompt already proves both identity (the person passes the phone's biometric) and enrolment health. Adding a PIN step before it would let a user enable biometrics without ever showing that the biometric works, which `app-login` wanted to avoid. The spec keeps the requirement as it was and adds the off rule beside it.

### D7. State and view model

`SecurityViewModel` (the Security section's view model from `app-login`, or a new one if the section was folded into a settings view model) gains:

```kotlin
data class SecurityUiState(
    val lockEnabled: Boolean,
    val biometricEnabled: Boolean,
    val biometricStatus: BiometricStatus,
    val verify: VerifyIdentityState,
    val message: SecurityMessage?,     // one-shot: PinChanged, LockDisabled, BiometricsOff
)
```

Events: `onChangePinTapped`, `onBiometricToggle(on)`, `onLockToggle(on)`, `onBiometricResult(result)`, `onPinSubmitted(pin)`, `onUsePin`, `onUseBiometrics`, `onVerifyDismissed`, `onPinChanged`. `Verified` is handled inside the view model by dispatching the request's `purpose` and returning `verify` to `Idle`, except for `CHANGE_PIN`, which emits a one-shot navigation effect to `PinSetup(CHANGE)`.

### D8. Package layout (additions to `app-login`'s)

```
applock/domain/  SecurityAction, IdentityMethod, VerifyIdentityRequest, VerifyIdentityState,
                 VerifyIdentity (use case), ChangePin (use case), AppLockRepository.replaceCredential()
applock/data/    DataStoreAppLockRepository.replaceCredential()
applock/ui/      VerifyIdentityDialog, SecurityViewModel additions, SecuritySection Change PIN row,
                 PinSetupScreen mode, PinSetupMode route argument
```

## Risks / Trade-offs

- [Biometric-verified PIN change weakens the PIN-only disable rule: change the PIN with a face, then disable with the new PIN] → Accepted per the request; class 2 biometrics were already accepted for unlocking, which exposes the same data. Recorded as an open question so the product owner can decide whether disable should also accept biometrics for consistency, or whether change PIN should be PIN-only.
- [Two prompts when the user wants both to change the PIN and turn biometrics off] → Accepted; these are rare actions and each is one glance or one PIN.
- [Replacing the disable dialog could regress `app-login` behaviour] → Its scenarios are restated unchanged in the spec and its tests move to the shared dialog.
- [Same-PIN check requires an HMAC evaluation at the first setup step] → Sub-millisecond on device and only in `CHANGE` mode.
- [Dialog with keypad at 200 percent font scale on small screens] → The dialog content scrolls and the keypad keeps 48 dp targets; covered by the manual test.
- [Race: relock while `replaceCredential` is mid-write] → The write is a single DataStore `edit`, which is atomic; either the old or the new credential is stored, never a mix.

## Migration Plan

No new persisted keys; `settingsVersion` stays at 1. Rollback is removing the Change PIN row, the `CHANGE` mode and `VerifyIdentity`, and restoring the confirm-PIN dialog for disable. A PIN changed in the meantime remains valid because the credential format is unchanged.

## Open Questions

- Should disabling the lock also accept a biometric now that the PIN can be changed after a biometric check, or should Change PIN be PIN-only to keep the PIN as the sole anchor? Recommended: keep as designed, revisit if the product owner wants a strict anchor.
- Should a successful check cover a short window (for example 30 seconds) so PIN change and biometric toggle can be done with one identification? Not until users ask.
