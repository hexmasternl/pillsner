# Manual test cases

These cannot be asserted automatically and are run by hand on a physical device before a release.

## Obsolete preconditions inherited from `reminder-delivery-reliability`

That change archived before this one was applied, so its `manual-tests.md` is read-only history and
is not edited. Two things in it are no longer true and are superseded here:

- **Its section 4, "The battery banner and its button (design D6)."** The battery-optimisation
  dialog it opens with no longer appears, and the banner it describes is neither raised by the same
  condition nor worded the same way. MT-2 below replaces it.
- **Its task 9.3 emulator precondition** — `adb shell dumpsys deviceidle whitelist +nl.hexmaster.pillsner`
  before an instrumented run, so the exemption dialog would not cover the UI. There is no dialog to
  cover the UI any more, so the whitelist step is obsolete. The instrumented suite needs
  `POST_NOTIFICATIONS` granted and nothing else.

## MT-1 No system dialog on first use with a scheduled medicine

**Setup.** A physical device with Pillsner freshly installed and **not** exempt from battery
optimisation (the default). Notifications allowed.

**Steps.** Add a medicine with a schedule that produces an upcoming dose. Return to Home. Leave the
app and come back to it several times.

**Expect.**
- No system dialog appears at any point — not on the first upcoming dose, not on any resume.
- Home shows the upcoming doses and **no banner at all**, although the app is not exempt.
- Settings → Apps → Pillsner shows no battery-optimisation permission for the app.

## MT-2 The banner appears only after a dose lapses un-reminded

The thing this change turns on cannot be provoked from the UI: it needs a reminder that the platform
genuinely failed to deliver. Force-stopping the app is the closest honest approximation.

**Setup.** A medicine scheduled a few minutes out, notifications allowed, the dose planned while it
was still in the future (so add the medicine before its time, not after).

**Steps.** Force-stop Pillsner from Android's app settings before the dose is due, and leave it
stopped past the moment the dose lapses. Open the app again.

**Expect.**
- The dose is recorded missed, and Home shows one banner reading "A dose came due and no reminder
  arrived."
- Its button reads "Check background settings" and opens the phone maker's auto-start screen where
  the device has one, or Android's battery-optimisation list otherwise. It must never open nothing.
- Coming back from that screen, the banner is gone, whether or not anything was changed there:
  tapping it is the acknowledgement.
- A later reminder arriving normally does not, by itself, bring the banner back. A further dose
  lapsing un-reminded does.

## MT-3 A dose generated already lapsed raises nothing

**Setup.** Notifications allowed, no missed-reminder record outstanding (acknowledge any banner
first).

**Steps.** At 20:00, add a medicine whose schedule includes 08:00. Today's 08:00 dose is generated
after its own moment. Wait for the next wake, or reopen the app.

**Expect.** The 08:00 dose is recorded missed, and **no banner appears**. There was never a reminder
to lose, so there is nothing to report.

## MT-4 Notifications denied takes precedence

**Setup.** Notifications denied for Pillsner, and a dose that lapses un-reminded while they are.

**Expect.** Home shows the notifications banner only. After granting notifications, no
missed-reminder banner appears in its place — the lapse had a known cause and was never recorded.
