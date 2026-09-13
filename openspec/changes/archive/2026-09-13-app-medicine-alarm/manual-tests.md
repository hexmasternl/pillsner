# Manual test cases

Reminders are the part of Pillsner that has to work when nobody is watching, and most of what can
go wrong is outside an automated test's reach. These are run by hand on a real device before a
release. Each one starts from a medicine of 40 mg every hour, unless it says otherwise.

## MT-1 A reminder on an idle device with the app killed

**Steps.** Force-stop Pillsner. Lock the phone and leave it untouched until a dose is due.

**Expect.** The reminder arrives within a minute of the scheduled time, wakes the screen, and shows
the three answers. Tapping "I took it" from the lock screen records the dose.

## MT-2 Reboot across a dose

**Steps.** Reboot the phone ten minutes before a dose is due. Do not open the app.

**Expect.** The reminder still arrives on time.

## MT-3 Reboot across a lapse

**Steps.** Let a reminder appear and do not answer it. Reboot before the next dose is due.

**Expect.** After the reboot the old reminder is gone by the time the next dose is due, and the old
dose reads as missed; the new one is announced normally.

## MT-4 Clock moved forward and backward

**Steps.** Turn off automatic time. Move the clock two hours forward, then back four hours.

**Expect.** Forward: any dose that is now in the past is announced once, and the app does not
announce doses it had already announced. Backward: nothing is re-announced, and the next reminder
still arrives at the right wall-clock time.

## MT-5 Time zone change

**Steps.** With a dose at 08:00 daily, change the phone's time zone from Amsterdam to Tokyo.

**Expect.** The dose is still at 08:00, now Tokyo time. No duplicate reminder appears for the same
day, and no stale reminder is left from the old zone.

## MT-6 The daylight-saving night

**Steps.** Set the clock to the evening before a transition, with a dose at 02:30, and let the
phone cross the boundary.

*Spring forward:* expect the reminder at 03:30, once.
*Autumn back:* expect the reminder once, at the first 02:30 of the night.

## MT-7 Answering from a Wear OS watch

**Setup.** A paired Wear OS watch.

**Expect.** The reminder appears on the watch with all three actions. "I took it" is the action the
watch offers first. Answering on the watch records the dose and removes the notification from the
phone as well.

## MT-8 Lock screen, sensitive content hidden and shown

**Steps.** Set notifications on the lock screen to "Hide sensitive content", then to "Show all".

**Expect.** Hidden: the reminder reads "Time for your medicine" with no name and no amount, and
still offers the three answers. Shown: the medicine name and amount are visible.

## MT-9 Largest font scale on the notification

**Steps.** System font scale at maximum, with a long medicine name.

**Expect.** The notification expands rather than truncating; the whole sentence is readable and all
three actions are reachable.

## MT-10 An aggressive battery optimiser

**Setup.** A device from a manufacturer known for killing background work (for example Xiaomi,
Huawei, OnePlus).

**Steps.** Leave the phone idle overnight with a morning dose scheduled.

**Expect.** The reminder arrives. If it does not, note the device and its battery settings: this is
the case the Home banner cannot detect, and it decides whether the app needs to ask for a battery
exemption.

## MT-11 Notifications turned off, then on again

**Steps.** Deny the notification permission. Open Home. Then turn notifications on in system
settings and return to the app.

**Expect.** The banner appears while notifications are off, its button opens the right settings
page, and the banner disappears as soon as the user comes back with them enabled.
