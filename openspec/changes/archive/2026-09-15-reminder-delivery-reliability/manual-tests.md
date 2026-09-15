# Manual tests

Four of this change's guarantees cannot be proved by an automated test. Each one needs a real phone
that has been left alone long enough for the platform to start throttling it, which is precisely the
condition the change exists to survive and precisely the condition an emulator does not reproduce.
They are written out here so they can be repeated exactly.

An emulator will not do for any of these. Run them on a physical device, and preferably on one of
the manufacturers named in design D6 — Samsung, Xiaomi, Oppo, OnePlus, Huawei — since their power
managers are what broke the old chain in the field.

## 1. A reminder arrives during a phone call (design D1, D2, D4)

Covers: *reminder-scheduling*, "Reminders fire from an exact alarm", scenario "App backgrounded
behind another task".

1. Add a medicine with one dose about ten minutes from now.
2. Confirm the alarm icon has appeared in the status bar.
3. Leave the app, and leave the phone alone for five minutes so it settles into the background.
4. Start a phone call that runs across the dose's moment.

**Expected:** the reminder arrives within the minute the dose is due, during the call, without the
app being opened. This is the exact scenario that failed in the field: the old
`setExactAndAllowWhileIdle` alarm was deferred behind the call, and because the only place the next
alarm was armed was inside the wake that alarm would have triggered, every later reminder stopped
too.

**Also check:** afterwards, that the reminder for the *next* dose still arrives on time. That is the
half of the fix that matters most — losing one alarm must cost one reminder, not all of them.

## 2. An overnight idle still produces the morning reminder (design D1)

Covers: *reminder-scheduling*, "Reminders fire from an exact alarm", scenario "Device idle".

1. Add a medicine with a dose due tomorrow morning.
2. Leave the phone unplugged, screen off and untouched overnight, deep enough into Doze that the
   platform has put the app into a restricted standby bucket. Do not force-stop it.

**Expected:** the reminder arrives within the minute it is due.

## 3. A reboot across a due dose, without unlocking (design D8)

Covers: *reminder-scheduling*, "Recovery after reboot and app update", scenario "Reboot before first
unlock with several doses ahead".

1. Add a medicine with two doses ahead of you, say one twenty minutes out and one twelve hours out.
2. Reboot the phone and **do not unlock it**.
3. Leave it locked across the first dose's moment.

**Expected:** the reminder for the first dose arrives while the phone is still locked, showing the
public version that says a medicine is due without naming it. Unlocking afterwards must not produce
a second copy of it, and the second dose's reminder must still arrive at its own time.

## 4. The battery banner and its button (design D6)

Covers: *reminder-delivery-resilience*, "The Home screen reports what is stopping reminders".

1. Add a medicine with a schedule. Accept nothing when the battery-optimisation dialog appears —
   or, if the exemption is already granted, revoke it in Android settings under Apps → Pillsner →
   Battery → Optimised.
2. Open the app.

**Expected:** Home shows one banner, the battery one, reading "Your phone may stop Pillsner from
running when a dose is due, so a reminder can be missed." with a button reading "Allow background
use". Tapping the button opens either the vendor's own auto-start screen, on a phone that ships one,
or Android's battery-optimisation list. It must never open nothing.

**Also check the precedence:** with notifications *also* turned off, the notification banner is the
one shown, and the battery banner appears only once notifications are back on. Only one banner is
ever on screen.

## Status

Not yet run. These need a physical device left idle for hours and a real phone call, neither of
which this change's automated suite can stand in for. The automated tests cover everything that can
be proved without one: the wake schedule, the repeat rule, the reconcile, the watchdog, the armed
record and the banner precedence.
