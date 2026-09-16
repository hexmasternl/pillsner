# Manual test cases

## MT-1 Locale change re-applies the cached language (task 3.3, design D3)

**Setup.** Language setting on "System default", phone set to English. Start the app so
`storedLanguage` is cached.

**Steps.** While the app is in the foreground, change the phone's system language to Dutch, then
return to the app.

**Expect.** The app switches to Dutch immediately, exactly as before this change. There is no
perceptible stall on the configuration-change callback (it no longer performs a DataStore read),
and no separate read is possible to observe directly since `applyStoredLanguage()`'s read only
ever runs once per process, at `startWhenUnlocked()`.

## MT-2 Cold start still paints the correct theme with no flash (task 4.2, design D4)

**Setup A.** Theme setting stored as Dark. Phone's system theme set to Light.

**Steps.** Force-stop the app, then cold-start it.

**Expect.** The very first frame is dark. No flash of a light frame first.

**Setup B.** Theme setting stored as Light. Phone's system theme set to Dark.

**Steps.** Force-stop the app, then cold-start it.

**Expect.** The very first frame is light. No flash of a dark frame first.

## MT-3 PIN unlock, PIN change and biometric fallback still work end to end (task 5.4)

**Setup.** A PIN is set; biometrics enabled where the device supports it.

**Steps.** Lock the app and unlock it with the correct PIN. Lock it again and enter a wrong PIN,
confirming the failure is registered and, after enough attempts, a cooldown appears. Open Settings
and change the PIN (proving identity first, then entering and confirming a new PIN). Trigger an
identity check (e.g. to disable the lock) and confirm the biometric prompt appears when enabled,
and that cancelling it falls back to the PIN keypad.

**Expect.** Every outcome matches current behaviour exactly — this change only moves the
Keystore/HMAC work off the main thread and removes a redundant key fetch; it does not change any
PIN, cooldown, or biometric decision.
