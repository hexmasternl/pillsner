## Why

`app-login` gives Pillsner an opt-in PIN lock with optional biometric unlock, but once a PIN is set the only way to change it is to disable the lock and set it up again, and the biometric preference can be switched off by anyone holding the already-unlocked phone. Users need to change their PIN in place, and every change to the lock's PIN or biometric settings needs proof that the person making it is the owner, not someone who picked up the phone while the app was open.

## What Changes

- Add a **Change PIN** action to the Security section, shown only while the PIN lock is enabled. It leads to the existing two-step choose-and-confirm PIN flow in a "new PIN" mode. The new PIN MUST differ from the current one. On success the lock stays enabled, the biometric preference is kept, the failed-attempt counter is reset and the user is told the PIN was changed.
- Add an **identity check** before sensitive Security changes. Changing the PIN and turning biometric unlock **off** first require the user to identify with the current PIN or, when biometric unlock is enabled and available, with a biometric. The check offers the biometric prompt first when it applies, with "Use PIN" as fallback, and shares the failed-attempt cooldown of the unlock screen.
- Turning biometric unlock **on** keeps its existing rule: one successful biometric prompt. That prompt both identifies the user and proves the enrolled biometric works, so no second check is added.
- Disabling the lock keeps its existing rule from `app-login`: current PIN only, biometrics not accepted. This change reuses the same identity-check component for it, configured to refuse biometrics.
- The identity check is valid for one action only. If the app goes to the background during any of these flows, the root lock gate takes over and the flow is abandoned with nothing changed.

## Capabilities

### New Capabilities

None. Everything here extends the existing lock behaviour.

### Modified Capabilities
- `app-lock`: "App lock is opt-in and off by default" gains the Change PIN control; "Biometric unlock can be enabled as an addition to the PIN" gains the rule that turning it off requires identification; "Disabling the lock requires the current PIN" is restated to use the shared identity check with biometrics refused. New requirements are added for changing the PIN, for the identity check itself and for abandoning flows on relock. This capability's spec is a delta inside the active `app-login` change; `app-login` MUST be archived before this change.

## Impact

- **Application code (`src/`)**: the `applock` feature area gains a `ChangePin` use case (verify current credential is replaced atomically by a new salt and verifier), a `VerifyIdentity` flow (view model state plus a `VerifyIdentityDialog` composable built on `PinKeypad` and the existing `BiometricAuthenticator`), a "Change PIN" row in `SecuritySection`, a `mode` for `PinSetupScreen` (set up versus change), and a rule in the domain that a new PIN must not verify against the current credential. The existing disable-lock confirm dialog is replaced by the shared `VerifyIdentityDialog` with biometrics disabled.
- **Dependencies**: none added. Uses `androidx.biometric`, DataStore and Compose already introduced by `app-login`.
- **Depends on**: `app-login` applied and archived first. `app-settings-language` and `app-login` both touch the Settings screen; the Security section keeps its position and only gains a row.
- **Persistence**: no new keys in the `applock` DataStore. Changing the PIN overwrites the salt and verifier in one transaction and resets the failure count.
- **Tests**: unit tests for `ChangePin` (atomic replacement, same-PIN rejection, counter reset, biometric preference preserved) and for the identity-check state machine (biometric first when eligible, PIN fallback, cooldown shared, one action per verification, refusal of biometrics for disable); Compose tests for the Change PIN row visibility, the full change-PIN flow, biometric-off requiring identification, and abandonment on relock; the existing disable tests move to the shared dialog.
- **README**: the app lock line in "Privacy by default" mentions that the PIN can be changed and that lock changes require re-identification.

## Non-goals

- Accepting biometrics for disabling the lock. `app-login`'s PIN-only rule stands; see the design's open question on whether that still adds protection once the PIN can be changed after a biometric check.
- A re-authentication session that keeps the Security section unlocked for a while. Each sensitive action verifies separately.
- Changing the PIN from the unlock screen, "forgot PIN" flows or recovery changes. Recovery remains as `app-login` defines it.
- PIN strength rules beyond length and difference from the current PIN.
