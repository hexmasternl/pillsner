# app-lock Specification

## Purpose
TBD - created by archiving change app-login. Update Purpose after archive.
## Requirements
### Requirement: App lock is opt-in and off by default
The app SHALL start with the app lock disabled. The app SHALL offer a Security section in its settings containing a "Protect with PIN" control, a "Change PIN" action shown only while the PIN lock is enabled, and an "Unlock with biometrics" control. The biometric control SHALL be disabled while the PIN lock is off.

#### Scenario: Fresh install
- **WHEN** the app is opened for the first time
- **THEN** no lock screen is shown and the Security section shows "Protect with PIN" off, no "Change PIN" action, and "Unlock with biometrics" off and not selectable

#### Scenario: Lock enabled
- **WHEN** the PIN lock is enabled and the user opens the Security section
- **THEN** the "Change PIN" action is shown between "Protect with PIN" and "Unlock with biometrics"

### Requirement: Enabling the lock requires choosing and confirming a PIN
When the user turns on "Protect with PIN", the app SHALL start a PIN setup flow that asks for a PIN and then asks the user to enter the same PIN again. A PIN SHALL consist of 4 to 6 decimal digits. The lock SHALL only become enabled once both entries match. Leaving the flow before confirmation SHALL leave the lock disabled.

#### Scenario: PIN confirmed
- **WHEN** the user enters a 4 to 6 digit PIN and then enters the identical PIN on the confirmation step
- **THEN** the lock is enabled, the user returns to the Security section, and "Protect with PIN" shows as on

#### Scenario: Confirmation does not match
- **WHEN** the user enters a PIN and then enters a different PIN on the confirmation step
- **THEN** the app shows a mismatch message, clears both entries and restarts the flow at the first step, and the lock remains disabled

#### Scenario: PIN too short
- **WHEN** the user tries to continue with fewer than 4 digits
- **THEN** the app does not accept the entry and indicates the minimum length

#### Scenario: Setup abandoned
- **WHEN** the user navigates back or the app goes to the background before the confirmation step succeeds
- **THEN** the lock remains disabled and no partial PIN is stored

### Requirement: The PIN is stored only as a device-bound verifier
The app SHALL NOT persist the PIN in plain text or in any reversible form. The app SHALL persist only a verifier computed from the PIN and a random salt using a key held in the Android Keystore that cannot be exported. Lock settings SHALL be excluded from Android auto backup and device-to-device transfer.

#### Scenario: Lock enabled
- **WHEN** the lock has been enabled with a PIN
- **THEN** the persisted lock settings contain the enabled flag, the salt and the verifier, and contain no field from which the PIN can be recovered without the Keystore key

#### Scenario: Backup rules
- **WHEN** the app's backup rules are inspected
- **THEN** the lock settings storage is listed as excluded for both cloud backup and device transfer

### Requirement: The app is locked on cold start
When the lock is enabled, the app SHALL show the unlock screen instead of its content when it is started from a terminated state. No app content SHALL be rendered or visible behind or before the unlock screen.

#### Scenario: Cold start with lock enabled
- **WHEN** the lock is enabled and the user launches the app from a terminated state
- **THEN** the unlock screen is the first screen shown and the app's content is not composed until unlock succeeds

#### Scenario: Cold start with lock disabled
- **WHEN** the lock is disabled and the user launches the app
- **THEN** the app opens directly on its content with no unlock screen

### Requirement: The app relocks when it returns to the foreground
When the lock is enabled, the app SHALL enter the locked state as soon as the whole app goes to the background (no activity of the app is started), and SHALL show the unlock screen when the app returns to the foreground. There SHALL be no grace period. In-app system dialogs that only pause the activity, such as the biometric prompt or a permission dialog, SHALL NOT trigger a relock.

#### Scenario: Return from another app
- **WHEN** the lock is enabled, the user switches to another app or the home screen and then returns to the app
- **THEN** the unlock screen is shown and the previously visible content is not shown until unlock succeeds

#### Scenario: Screen turned off and on
- **WHEN** the lock is enabled, the screen is turned off while the app is in the foreground and the device is unlocked again
- **THEN** the unlock screen is shown

#### Scenario: Biometric prompt does not relock
- **WHEN** the biometric prompt is shown over the app and is then dismissed
- **THEN** the app does not enter a new locked state as a result of the prompt being shown

#### Scenario: Unlock preserves navigation state
- **WHEN** the user unlocks after a relock
- **THEN** the app shows the screen the user was on before the app went to the background

### Requirement: Entering the correct PIN unlocks the app
The unlock screen SHALL present a numeric entry that accepts 4 to 6 digits and a way to submit. Entering the PIN that matches the stored verifier SHALL unlock the app. Entering a PIN that does not match SHALL clear the entry, show an error and increase the failed-attempt count. The entered digits SHALL be masked.

#### Scenario: Correct PIN
- **WHEN** the user enters the PIN that was set up and submits
- **THEN** the app unlocks and the failed-attempt count is reset to zero

#### Scenario: Wrong PIN
- **WHEN** the user enters a PIN that does not match and submits
- **THEN** the entry is cleared, a wrong-PIN message is shown and the failed-attempt count increases by one

### Requirement: Repeated wrong PIN entries trigger a cooldown
After 5 consecutive failed PIN attempts the unlock screen SHALL refuse further PIN entry for 30 seconds and show the remaining time. Each further block of 5 consecutive failures SHALL double the cooldown, up to a maximum of 5 minutes. The failed-attempt count and cooldown end time SHALL survive the app being terminated and restarted. A successful unlock SHALL reset the count and cooldown.

#### Scenario: Fifth consecutive failure
- **WHEN** the user submits a wrong PIN for the fifth consecutive time
- **THEN** PIN entry is disabled, a countdown of 30 seconds is shown and biometric unlock remains available if enabled

#### Scenario: Cooldown survives restart
- **WHEN** the app is terminated and restarted during an active cooldown
- **THEN** the unlock screen shows the remaining cooldown and refuses PIN entry until it has elapsed

#### Scenario: Cooldown escalates
- **WHEN** the user fails 5 more consecutive times after a 30 second cooldown has elapsed
- **THEN** the next cooldown lasts 60 seconds

### Requirement: Biometric unlock can be enabled as an addition to the PIN
The user SHALL be able to turn on "Unlock with biometrics" only when the PIN lock is enabled and the device reports that a biometric of class 2 (weak) or stronger is enrolled. Turning it on SHALL require one successful biometric authentication. Turning it off SHALL require the identity check with biometrics allowed; the control SHALL remain on until the check succeeds. When the device reports no enrolled biometric or no biometric hardware, the control SHALL be shown disabled with an explanation.

#### Scenario: Enable biometrics
- **WHEN** the PIN lock is enabled, a biometric is enrolled and the user turns on "Unlock with biometrics" and passes the biometric prompt
- **THEN** biometric unlock is enabled and the control shows as on

#### Scenario: Biometric prompt cancelled during enable
- **WHEN** the user turns on "Unlock with biometrics" and cancels or fails the biometric prompt
- **THEN** biometric unlock remains disabled and the control shows as off

#### Scenario: No biometric enrolled
- **WHEN** the PIN lock is enabled and the device has no enrolled biometric
- **THEN** "Unlock with biometrics" is shown disabled with a message that no biometric is set up on the device

#### Scenario: Disable biometrics with a biometric
- **WHEN** biometric unlock is on and the user turns it off and passes the biometric prompt of the identity check
- **THEN** biometric unlock is disabled and the control shows as off

#### Scenario: Disable biometrics with the PIN
- **WHEN** biometric unlock is on and the user turns it off, chooses "Use PIN" and enters the current PIN
- **THEN** biometric unlock is disabled and the control shows as off

#### Scenario: Disable biometrics cancelled
- **WHEN** biometric unlock is on, the user turns it off and dismisses the identity check
- **THEN** biometric unlock remains enabled and the control still shows as on

### Requirement: A successful biometric check bypasses the PIN
When biometric unlock is enabled and the unlock screen is shown, the app SHALL automatically present the biometric prompt. A successful biometric authentication SHALL unlock the app without a PIN. Cancelling or failing the prompt SHALL leave the user on the unlock screen with PIN entry available, and the user SHALL be able to trigger the prompt again.

#### Scenario: Biometric success
- **WHEN** the unlock screen is shown with biometric unlock enabled and the user passes the biometric prompt
- **THEN** the app unlocks without asking for the PIN

#### Scenario: Biometric cancelled
- **WHEN** the user dismisses the biometric prompt with the "Use PIN" action
- **THEN** the unlock screen remains with PIN entry active and a control to retry biometrics

#### Scenario: Biometric locked out by the system
- **WHEN** the system reports that biometrics are temporarily or permanently locked out
- **THEN** the unlock screen shows PIN entry and does not repeatedly re-present the prompt

### Requirement: Biometric unlock falls back to PIN when biometrics become unavailable
If biometric unlock is enabled but the device later reports no enrolled biometric or no biometric hardware, the unlock screen SHALL show only PIN entry and the Security section SHALL show "Unlock with biometrics" as off and disabled with an explanation. The stored biometric preference SHALL be turned off when this is detected.

#### Scenario: Biometrics unenrolled after enabling
- **WHEN** biometric unlock is enabled and the user removes all biometrics from the device settings, then opens the app
- **THEN** the unlock screen shows PIN entry only and the biometric setting is turned off

### Requirement: Disabling the lock requires the current PIN
Turning off "Protect with PIN" SHALL run the identity check with biometrics refused, so the user must enter the current PIN. On success the lock, the biometric preference, the salt and the verifier SHALL be removed and the Security section SHALL show both controls off and no "Change PIN" action. On a wrong PIN the lock SHALL remain enabled and the same failed-attempt cooldown SHALL apply as on the unlock screen. Biometric authentication SHALL NOT be accepted for disabling the lock.

#### Scenario: Disable with correct PIN
- **WHEN** the user turns off "Protect with PIN" and enters the current PIN
- **THEN** the lock is disabled, biometric unlock is disabled and no verifier remains stored

#### Scenario: Disable with wrong PIN
- **WHEN** the user turns off "Protect with PIN" and enters a wrong PIN
- **THEN** the lock remains enabled, the control still shows as on and the failed-attempt count increases

#### Scenario: Disable while cooling down
- **WHEN** a PIN cooldown is active and the user tries to turn off "Protect with PIN"
- **THEN** PIN entry is refused and the remaining cooldown is shown

#### Scenario: No biometric option when disabling
- **WHEN** biometric unlock is enabled and the user turns off "Protect with PIN"
- **THEN** the identity check shows PIN entry only, with no biometric prompt and no "Use biometrics" control

### Requirement: The app hides its content from the recents screen while locked
While the lock is enabled, the app SHALL prevent its window content from appearing in the recents thumbnail and in screenshots. When the lock is disabled this restriction SHALL be lifted.

#### Scenario: Recents with lock enabled
- **WHEN** the lock is enabled and the user opens the recents screen
- **THEN** the app's thumbnail shows no app content

#### Scenario: Recents with lock disabled
- **WHEN** the lock is disabled and the user opens the recents screen
- **THEN** the app's thumbnail shows the app content as normal

### Requirement: The lock can be recovered when the verifier cannot be evaluated
If the lock is enabled but the Keystore key required to evaluate the verifier is missing or invalidated, the unlock screen SHALL explain that the PIN can no longer be checked and SHALL offer to reset the app lock by confirming the device screen lock (PIN, pattern, password or device biometric). On success the app lock SHALL be disabled and the user SHALL be told to set it up again. On failure or cancel the app SHALL remain locked.

#### Scenario: Key missing
- **WHEN** the lock is enabled, the Keystore key is absent and the user opens the app
- **THEN** the unlock screen shows the recovery explanation and a "Reset app lock" action instead of PIN entry

#### Scenario: Recovery confirmed
- **WHEN** the user chooses "Reset app lock" and passes the device credential prompt
- **THEN** the app lock is disabled, the app opens on its content and the Security section shows "Protect with PIN" off

#### Scenario: Recovery cancelled
- **WHEN** the user chooses "Reset app lock" and cancels the device credential prompt
- **THEN** the app stays on the unlock screen in the recovery state

### Requirement: The unlock and setup screens are accessible
The PIN entry SHALL be operable with TalkBack, SHALL respect the system font scale up to 200 percent without clipping, and SHALL be reachable one-handed with the keypad in the lower part of the screen. Error and cooldown messages SHALL be announced to screen readers.

#### Scenario: Large font
- **WHEN** the system font scale is set to 200 percent
- **THEN** all digits, controls and messages on the unlock and setup screens remain fully visible and operable

#### Scenario: Screen reader
- **WHEN** TalkBack is enabled and a wrong PIN is submitted
- **THEN** the wrong-PIN message is announced

### Requirement: Identity check before security changes
Before changing the PIN, turning biometric unlock off, or disabling the lock, the app SHALL require the user to identify. When the action allows biometrics, biometric unlock is enabled and the device reports a usable biometric, the check SHALL present the biometric prompt first with a "Use PIN" fallback; otherwise it SHALL present PIN entry. A biometric failure, cancel or lockout SHALL fall back to PIN entry without changing the biometric preference. A wrong PIN SHALL count towards the same failed-attempt cooldown as the unlock screen and an active cooldown SHALL refuse PIN entry. A successful check SHALL authorise exactly one action; a further sensitive action SHALL require a new check. Dismissing the check SHALL change nothing.

#### Scenario: Biometric offered first
- **WHEN** biometric unlock is enabled, a biometric is available and the user taps "Change PIN"
- **THEN** the biometric prompt is shown with a "Use PIN" option

#### Scenario: PIN only when biometrics are off
- **WHEN** biometric unlock is disabled and the user taps "Change PIN"
- **THEN** PIN entry is shown and no biometric prompt appears

#### Scenario: Biometric failure falls back to PIN
- **WHEN** the biometric prompt of the check fails or is locked out by the system
- **THEN** PIN entry is shown, the biometric preference is unchanged, and a "Use biometrics" control allows a retry unless the system reported a lockout

#### Scenario: Wrong PIN in the check
- **WHEN** the user enters a wrong PIN in the identity check
- **THEN** the entry is cleared, a wrong-PIN message is announced and the shared failed-attempt count increases by one

#### Scenario: Cooldown in the check
- **WHEN** five consecutive wrong PINs have been entered across the unlock screen and identity checks
- **THEN** the check refuses PIN entry and shows the remaining cooldown

#### Scenario: One action per check
- **WHEN** the user passes the check to turn biometric unlock off and then taps "Change PIN"
- **THEN** a new identity check is required before the PIN flow starts

#### Scenario: Dismissed check
- **WHEN** the user dismisses the identity check by pressing back or cancelling
- **THEN** no setting changes and the Security section shows its previous values

### Requirement: Changing the PIN
After a successful identity check, "Change PIN" SHALL start the two-step choose-and-confirm flow titled for a new PIN. The new PIN SHALL be 4 to 6 digits and SHALL NOT match the current PIN. When both entries match, the app SHALL replace the stored salt and verifier atomically, keep the lock enabled, keep the biometric preference, reset the failed-attempt count and cooldown, return to the Security section and confirm that the PIN was changed. Leaving the flow before confirmation SHALL keep the current PIN.

#### Scenario: PIN changed
- **WHEN** the user passes the identity check, enters a new PIN and confirms it identically
- **THEN** the new PIN unlocks the app, the old PIN does not, biometric unlock keeps its previous value and a "PIN changed" confirmation is shown

#### Scenario: New PIN equals current PIN
- **WHEN** the user enters the current PIN as the new PIN at the first step
- **THEN** the entry is rejected with a message to choose a different PIN and the flow stays at the first step

#### Scenario: Confirmation mismatch
- **WHEN** the confirmation entry differs from the new PIN
- **THEN** a mismatch message is shown, both entries are cleared, the flow restarts at the first step and the current PIN remains valid

#### Scenario: Change abandoned
- **WHEN** the user navigates back before confirming the new PIN
- **THEN** the current PIN remains valid and no partial PIN is stored

#### Scenario: Failed attempts reset
- **WHEN** the failed-attempt count is three and the user changes the PIN successfully
- **THEN** the failed-attempt count is zero and no cooldown is active

### Requirement: Security flows are abandoned on relock
If the app goes to the background during an identity check or a change-PIN flow, the app SHALL relock as usual, the check or flow SHALL be discarded and no lock setting SHALL change.

#### Scenario: Background during the identity check
- **WHEN** the identity check is showing and the user switches to another app and returns
- **THEN** the unlock screen is shown, and after unlocking the Security section shows no pending check and unchanged settings

#### Scenario: Background during change PIN
- **WHEN** the user has entered a new PIN but not confirmed it and the app goes to the background
- **THEN** after unlocking with the current PIN the Security section is shown and the current PIN is still the valid one

### Requirement: Security section changes are accessible
The "Change PIN" action SHALL be operable with TalkBack and announce its purpose; the identity check SHALL announce its title and errors; all controls SHALL remain visible and operable at 200 percent font scale.

#### Scenario: Screen reader on Change PIN
- **WHEN** TalkBack focuses the "Change PIN" row
- **THEN** it announces "Change PIN" and that it is a button

#### Scenario: Large font in the identity check
- **WHEN** the system font scale is 200 percent and the identity check shows PIN entry
- **THEN** the keypad, the message area and the cancel control are fully visible and operable

### Requirement: PIN verification does not block the UI thread
Checking a PIN against its stored verifier SHALL run off the main thread. The unlock screen, the identity check and the change-PIN flow SHALL remain responsive (input keeps working, no dropped frames) while a PIN check, PIN creation, or Keystore key resolution is in progress.

#### Scenario: Unlock screen stays responsive during verification
- **WHEN** the user submits a PIN on the unlock screen
- **THEN** the PIN check runs off the main thread and the screen continues to accept input and render without a stall

#### Scenario: Identity check stays responsive during verification
- **WHEN** the user submits a PIN during an identity check before changing the PIN or disabling the lock
- **THEN** the PIN check runs off the main thread and the identity check screen remains responsive

