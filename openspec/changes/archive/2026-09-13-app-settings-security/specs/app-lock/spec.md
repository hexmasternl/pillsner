## MODIFIED Requirements

### Requirement: App lock is opt-in and off by default
The app SHALL start with the app lock disabled. The app SHALL offer a Security section in its settings containing a "Protect with PIN" control, a "Change PIN" action shown only while the PIN lock is enabled, and an "Unlock with biometrics" control. The biometric control SHALL be disabled while the PIN lock is off.

#### Scenario: Fresh install
- **WHEN** the app is opened for the first time
- **THEN** no lock screen is shown and the Security section shows "Protect with PIN" off, no "Change PIN" action, and "Unlock with biometrics" off and not selectable

#### Scenario: Lock enabled
- **WHEN** the PIN lock is enabled and the user opens the Security section
- **THEN** the "Change PIN" action is shown between "Protect with PIN" and "Unlock with biometrics"

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

## ADDED Requirements

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
