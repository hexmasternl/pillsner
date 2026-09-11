## Why

Pillsner keeps medication data on the device, but anyone who picks up an unlocked phone can currently open the app and read it. Medication names, dosages and intake history are sensitive health information, so the user needs an optional, in-app lock that protects this data without weakening the "keep the data on the device" promise. The lock needs to exist before the medication and intake screens ship so that every later screen is gated from the start rather than retrofitted.

## What Changes

- Add an opt-in **app lock** that the user enables from a new Security section in the app's settings.
- Enabling the lock requires the user to choose a numeric PIN and confirm it by entering it a second time. Mismatches are reported and the user retries.
- Once enabled, the app shows a lock screen instead of its content on every cold start and every time the app returns to the foreground after having gone to the background. The user must enter the PIN to proceed.
- The user may additionally enable **biometric unlock** (fingerprint, face or other enrolled biometric). When enabled, a successful biometric check unlocks the app and bypasses the PIN. The PIN always remains available as a fallback and can never be removed while the lock is enabled.
- Biometric unlock can only be enabled once a PIN exists, and only on devices that report an enrolled biometric. If biometrics later become unavailable or unenrolled, the app falls back to the PIN.
- Disabling the lock requires entering the current PIN. Disabling the lock also disables biometric unlock.
- Repeated wrong PIN entries trigger a temporary cooldown to slow guessing.
- While the lock is enabled, the app's content is hidden from the recents (task switcher) thumbnail.
- The PIN is never stored in plain text. Only a keyed verifier derived from the PIN is persisted, and lock settings are excluded from cloud backup so that they never leave the device.
- If the lock configuration can no longer be verified (for example the device-bound key is gone), the user can reset the lock by confirming the device screen lock, after which the app lock is disabled and must be set up again. This prevents permanent lockout of the user's own data.

## Capabilities

### New Capabilities
- `app-lock`: Opt-in protection of the app with a PIN and optional biometric unlock, covering setup, enforcement on launch and on return to foreground, unlock, failed-attempt cooldown, disabling, and recovery when the lock can no longer be verified.

### Modified Capabilities

None. `openspec/specs/` contains no capabilities yet.

## Impact

- **Application code (`src/`)**: new `applock` feature area spanning UI (Compose lock screen, PIN setup flow, Security settings section), domain (lock state, PIN verifier, lock policy) and data (lock settings storage, device-bound verifier key). A root-level gate in the single activity renders the lock screen instead of the navigation host while the app is locked.
- **Project scaffold**: `src/` is currently empty. Implementing this change requires a minimal Gradle project with a single Compose activity, navigation and a Settings screen to host the Security section. This change creates that scaffold only if it is still absent when the change is applied, and keeps it to the minimum needed.
- **Dependencies** (all AndroidX, first party): `androidx.biometric` for the biometric prompt and availability checks, `androidx.datastore` (Preferences) for lock settings, `androidx.lifecycle:lifecycle-process` for foreground/background detection. No third-party libraries, no network access, no telemetry.
- **Platform APIs**: Android Keystore for the device-bound key that derives the PIN verifier, `FLAG_SECURE` on the activity window while the lock is enabled, and `BiometricPrompt` with device-credential fallback for the recovery path.
- **Backup rules**: the app declares backup rules that exclude the lock settings file from Android auto backup and device-to-device transfer.
- **Future changes**: reminder notifications, medication and intake screens all inherit the gate automatically. Whether confirming a dose from a notification action should require unlocking is deliberately left to the reminders change.

## Non-goals

- Changing the PIN in place (achievable by disabling and re-enabling the lock; a dedicated flow can follow later).
- A grace period that keeps the app unlocked for a short time after it goes to the background. The lock engages immediately, as requested.
- Encrypting the Room database itself. The lock gates access to the UI; storage-at-rest encryption is a separate concern.
- Any account, cloud sync or remote reset.
