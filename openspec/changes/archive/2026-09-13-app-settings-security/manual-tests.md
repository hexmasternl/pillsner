# Manual tests: app-settings-security

What automated tests cannot reach: the real biometric prompt, the system's own lockout, and what
a screen reader actually says. Run these on a physical device with a fingerprint or face enrolled,
in both English and Dutch. Every case starts from Settings, Security, with "Protect with PIN" on
and the PIN set to 1234 unless a case says otherwise.

## 1. Change the PIN with a biometric

1. Turn "Unlock with biometrics" on and pass the prompt.
2. Tap **Change PIN**.
3. **Expect:** the biometric prompt appears with "Confirm it's you to change your PIN" behind it
   and a "Use PIN" button under it.
4. Pass the prompt.
5. **Expect:** the "Choose a new PIN" screen.
6. Enter 5678, Continue, enter 5678, Continue.
7. **Expect:** back on Settings with a "PIN changed" message; "Protect with PIN" and "Unlock with
   biometrics" are both still on.
8. Send the app to the background and return.
9. **Expect:** the unlock screen. 1234 is refused; 5678 unlocks.

## 2. Change the PIN with the PIN

1. Turn "Unlock with biometrics" off first (case 5), or use a device with none enrolled.
2. Tap **Change PIN**.
3. **Expect:** no biometric prompt; the keypad appears straight away under "Confirm it's you to
   change your PIN".
4. Enter the current PIN, Continue.
5. **Expect:** the "Choose a new PIN" screen.
6. Enter the PIN you already have, Continue.
7. **Expect:** "Choose a PIN that differs from your current one." and the first step again.
8. Enter a different PIN twice.
9. **Expect:** "PIN changed", and only the new PIN unlocks the app afterwards.

## 3. The old PIN really stops working

1. Change the PIN from 1234 to 5678.
2. Lock the app (background and return).
3. Enter 1234.
4. **Expect:** "That PIN doesn't match. Try again." — the app stays locked.
5. Enter 5678.
6. **Expect:** the app opens.

## 4. Biometric prompt cancelled during the check

1. With biometrics on, tap **Change PIN** and cancel the prompt.
2. **Expect:** the keypad appears in the same dialog, with a "Use biometrics" button below it.
3. **Expect:** "Unlock with biometrics" is still on in the Security section behind the dialog.
4. Tap "Use biometrics" and pass the prompt.
5. **Expect:** the "Choose a new PIN" screen.

## 5. Turn biometrics off

1. With biometrics on, turn the "Unlock with biometrics" switch off.
2. **Expect:** the biometric prompt for "Confirm it's you to turn off biometric unlock"; the
   switch has not moved.
3. Cancel it, then cancel the dialog.
4. **Expect:** the switch is still on and nothing changed.
5. Turn it off again and pass the check (either way).
6. **Expect:** the switch is off and a "Biometric unlock turned off" message is shown.

## 6. Turn the lock off: PIN only

1. With biometrics on, turn "Protect with PIN" off.
2. **Expect:** the keypad, with no biometric prompt and no "Use biometrics" button, under
   "Confirm it's you to turn off the app lock".
3. Enter a wrong PIN.
4. **Expect:** the wrong-PIN message; the switch is still on.
5. Enter the correct PIN.
6. **Expect:** both switches are off, the "Change PIN" row is gone, and the app opens without a
   lock next time.

## 7. One check, one action

1. Turn biometrics off through the check (case 5).
2. Immediately tap **Change PIN**.
3. **Expect:** the keypad appears again — the check just passed does not carry over.

## 8. The cooldown is shared

1. On the unlock screen, enter four wrong PINs.
2. Unlock with the correct PIN, go to Settings, turn "Protect with PIN" off and enter one wrong PIN.
3. **Expect:** "Too many wrong attempts. Try again in 30 seconds." and a keypad that does nothing.
4. Wait it out.
5. **Expect:** the correct PIN turns the lock off.

## 9. Too many biometric failures

1. With biometrics on, tap **Change PIN** and fail the biometric five times until the system says
   it is locked out.
2. **Expect:** the keypad appears and there is **no** "Use biometrics" button.
3. **Expect:** "Unlock with biometrics" is still on in the Security section.

## 10. Background during the check

1. Tap **Change PIN** and leave the dialog open.
2. Switch to another app and come back.
3. **Expect:** the unlock screen. After unlocking, Settings shows no dialog and unchanged switches.

## 11. Background during the change

1. Pass the check, enter a new PIN at the first step, and go to the background before confirming.
2. Come back and unlock.
3. **Expect:** the current (old) PIN still unlocks the app, and the PIN flow is gone.

## 12. Rotation and process death

1. Tap **Change PIN**, pass the check, and rotate the device on the "Choose a new PIN" screen.
2. **Expect:** the screen survives; the entry is cleared, which is intended — nothing half-typed
   is kept.
3. With "Don't keep activities" on, repeat: after the process is recreated the app is locked and
   the current PIN is still the valid one.

## 13. TalkBack

1. Turn TalkBack on.
2. Swipe to the **Change PIN** row.
3. **Expect:** "Change PIN, Choose a new PIN for the app lock, button".
4. Open the check and enter a wrong PIN.
5. **Expect:** the wrong-PIN message is announced without moving focus.
6. **Expect:** every keypad key, "Cancel" and the fallback buttons are reachable and labelled.

## 14. 200 percent font scale

1. Set the system font size to its largest.
2. Open the identity check and the "Choose a new PIN" screen.
3. **Expect:** the title wraps rather than truncating, the keypad keys stay at least 48 dp, the
   dialog content scrolls, and "Cancel" is reachable.

## 15. Dutch

1. Set the app language to Nederlands and restart it.
2. Walk cases 1, 2 and 6.
3. **Expect:** "Pincode wijzigen", "Bevestig dat jij het bent om je pincode te wijzigen", "Kies een
   nieuwe pincode", "Pincode gewijzigd" — no English is left anywhere in the flow.
