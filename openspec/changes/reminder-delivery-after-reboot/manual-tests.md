# Manual tests

Three of this change's guarantees cannot be proved by an automated test, because each needs a real
device to reboot, to sit locked, or to have a permission taken away and given back. They are written
out here so they can be repeated exactly.

## 1. A dose that fell due without notification permission is still announced (design D1)

Covers: *reminder-scheduling*, "A dose is only recorded as reminded when it was announced", scenario
"Permission granted before the dose lapses".

1. Add a medicine with one dose a few minutes from now.
2. In Android settings, turn Pillsner's notifications off.
3. Wait for the dose's moment to pass. Confirm no notification arrives and that the Home screen
   shows the banner saying reminders cannot be shown.
4. Turn Pillsner's notifications back on, well inside the dose's lapse window (the next dose of the
   same medicine, or 24 hours).
5. Open the app.

**Expected:** the reminder for that dose is posted. Before this change the dose had been recorded as
reminded while nothing was shown, and it went silently to missed.

## 2. A reboot overnight still produces the morning reminder (design D3, D5)

Covers: *reminder-scheduling*, "Recovery after reboot and app update", scenarios "Reboot before first
unlock" and "First unlock after reboot".

1. Add a medicine with a dose due tomorrow morning.
2. Reboot the phone in the evening and **do not unlock it**.
3. Leave it locked, face down, overnight.

**Expected:** the reminder arrives at the dose's moment, with the phone still locked. On the lock
screen it shows the public version — that a medicine is due, not which one.

Quicker variant, same path: reboot the phone, do not unlock, and check
`adb shell dumpsys alarm | grep pillsner` — an alarm is listed before the first unlock.

## 3. A reboot across a dose catches up at the first unlock (design D4)

Covers: *reminder-scheduling*, "The wake cycle does not run while the user is locked".

1. Add a medicine with a dose due in ten minutes.
2. Power the phone off before the dose is due, and power it back on after its moment has passed.
3. Leave it locked for a minute, then unlock it.

**Expected:** the app does not crash while locked, no dose is recorded as missed during that minute,
and the reminder for the dose appears at or just after the unlock.
