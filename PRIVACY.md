# Privacy Statement

**Pillsner — your partner in taking your pills.**

| | |
| --- | --- |
| Applies to | The Pillsner Android app and its Wear OS companion app (`nl.hexmaster.pillsner`) |
| Version | 1 |
| Effective from | 14 September 2026 |
| Published by | Eduard Keilholz, the Netherlands |

---

## The short version

Pillsner keeps your medication data on your own device. It has no account, no cloud sync, no
analytics, no advertising and no crash reporting. Neither the phone app nor the watch app asks for
the `INTERNET` permission, so neither of them can send anything to a server even if it wanted to.
Nobody — including the publisher — can see what medicines you take.

There is exactly one case in which data leaves your phone: if you install the Wear OS companion
app, the doses you still have to take travel over the direct link between your phone and your
paired watch. That is described in full in [Section 4](#4-the-one-case-where-data-leaves-your-phone).

One thing to be aware of: if you have Android's own backup switched on for your Google account,
Android may include Pillsner's data in that backup. See [Section 6](#6-android-backup).

---

## 1. Who is responsible

Pillsner is published and developed by **Eduard Keilholz**, in the Netherlands.

Because Pillsner stores everything on your device and transmits nothing to the publisher, the
publisher holds no personal data about you, receives none, and cannot retrieve any. In the terms of
the General Data Protection Regulation, there is no processing of your personal data by the
publisher to speak of. You remain in sole control of the data on your device.

## 2. What Pillsner stores on your device

Everything you enter, and everything the app records, is written to a database and to two small
preference files in the app's own private storage, which other apps cannot read.

| What | Where it is stored | What it holds |
| --- | --- | --- |
| Medicines | `pillsner.db` (Room/SQLite) | Name, default dose and unit, the dates you take it between, who prescribed it, and whether it is active |
| Schedules | `pillsner.db` | For each medicine: how often a dose is due, at what times or on which weekdays, and the amount |
| Doses and intakes | `pillsner.db` | For each planned dose: the medicine name and amount it was planned under, when it was due, the outcome you gave it (taken, skipped, missed), when you answered, and any snooze |
| Language choice | `settings` preferences file | Your chosen app language, or nothing if you follow the phone |
| Legal acceptance | `settings` preferences file | Which version of the Disclaimer and the Terms of Service you accepted, and the date |
| App lock | `applock` preferences file | Whether the lock is on, whether biometric unlock is on, and a random salt plus a verifier for your PIN — never the PIN itself (see [Section 7](#7-the-app-lock)) |

This is the complete list. Pillsner does not store your name, your date of birth, your condition,
your prescriptions, your doctor's details beyond the free-text "prescribed by" field you fill in
yourself, or any identifier for you or your device.

## 3. What Pillsner does not do

- **No account.** There is nothing to sign up for and nothing to sign in to.
- **No network access.** Neither app declares the `INTERNET` permission. No data can be uploaded,
  and no content can be downloaded.
- **No analytics, telemetry or usage statistics.** Nothing counts your taps or reports how you use
  the app.
- **No crash reporting.** No crash or error report is sent anywhere.
- **No advertising, and no advertising ID.** There is no ad SDK, and every release build is checked
  to confirm that no dependency has slipped an advertising-identifier permission into the app.
- **No third-party analytics, attribution or marketing libraries.** The libraries Pillsner uses are
  AndroidX and Kotlin first-party ones, plus Google Play services Wearable for the phone-to-watch
  link described below.
- **No location, contacts, camera, microphone, shared storage, call log, calendar or health-platform
  access.** None of these permissions is requested. Pillsner does not read from or write to Health
  Connect or any other health platform.

### The permissions Pillsner does ask for

| Permission | Why it is needed |
| --- | --- |
| `POST_NOTIFICATIONS` | A reminder is a notification. Without it Pillsner cannot remind you of anything, and the Home screen tells you so. |
| `USE_EXACT_ALARM` (Android 13+) and `SCHEDULE_EXACT_ALARM` (Android 12) | A dose due at 08:00 has to be announced at 08:00. Android reserves exact alarms for apps whose core function is alarms or reminders. Without it, reminders drift within a ten-minute window and the Home screen warns you. |
| `RECEIVE_BOOT_COMPLETED` | Restarting the phone clears every pending alarm, so Pillsner has to set its own again, or your reminders would silently stop. |
| `USE_BIOMETRIC` and `USE_FINGERPRINT` | Declared by the Android biometric library so the optional app lock can ask the system for a fingerprint or face check. It lets the app ask the question, not see the answer's raw data; your fingerprint and face never reach Pillsner. Unused if you never turn the lock on. |

That is the whole list, as it appears in the installed app. None of these permissions gives the app
access to anything about you beyond what you entered.

## 4. The one case where data leaves your phone

If you install the **Wear OS companion app** on a paired watch, the phone app sends the watch the
doses you still have to take, so the watch can show them.

- **What is sent:** for each pending dose, an internal dose number, the medicine's name, the amount
  written out as text ("40 mg"), and the time the dose is due. The message also carries the language
  the phone app is set to, so the watch reads in the same language.
- **How it is sent:** over the Wearable Data Layer, part of Google Play services. That travels over
  the direct Bluetooth or local-network link between your phone and the watch you paired it with. It
  does not go to a server, and neither app has permission to reach one.
- **Where it ends up:** Google Play services stores the message on the watch, so the list is there
  the moment you raise your wrist, including while the phone is out of range. It is replaced each
  time the phone sends a new one.
- **How to stop it:** uninstall the watch app. That removes the data it holds. If you never install
  the watch app, nothing is ever sent anywhere.
- Google Play services is Google's own software, running on your devices under your Google account
  and Google's terms. Pillsner uses it only as the pipe between two devices you own.

Your intake history, your inactive medicines, your schedules and your settings are never sent to the
watch. The watch only shows; every answer you give a dose is recorded on the phone.

## 5. Reminders and what they show

A reminder notification shows the medicine's name, the amount and the time, because that is what
makes it useful.

- On the lock screen, Pillsner marks its reminders as private. If your phone is set to hide
  sensitive notifications when locked, the reminder shows only a neutral line — no medicine name and
  no amount — until you unlock.
- If a watch is paired, Android bridges the phone's reminder to it, which is how you can answer a
  dose from your wrist. The reminder on the watch shows the same text as on the phone.
- Whether a reminder is shown, and how, is ultimately governed by your phone's notification
  settings, which you control.

## 6. Android backup

Pillsner leaves Android's standard backup behaviour in place. That means that **if you have backup
switched on for your Google account**, Android may include Pillsner's database and its general
settings in the backup it makes of your phone, and restore them when you set up a new device. That
backup is made by Android, stored under your own Google account, and governed by Google's terms —
not by Pillsner, which has no access to it and never sees it.

- The app lock's file is **excluded** from backup and from device-to-device transfer on every
  supported Android version. Its PIN verifier can only be checked with a key that never leaves the
  device it was created on, so restoring it elsewhere would only produce a lock nobody could open.
  After a restore, the app lock is off and can be set up again.
- If you would rather Pillsner's data were never included in a backup, turn off Android backup for
  the app, or for the device, in your phone's system settings. Where Android offers a per-app switch,
  it is under **Settings → Google → Backup**.

Keeping a backup, or deciding not to, is your choice and your responsibility. The publisher cannot
recover your data for you under any circumstances.

## 7. The app lock

Pillsner offers an optional lock that asks for a PIN, and for a fingerprint or face where your phone
supports one, before the app opens.

- **Your PIN is never stored.** What is stored is a random salt and a verifier computed from your
  PIN with a key that is generated inside the Android Keystore and cannot be exported from your
  device. Copying the file to another machine yields nothing usable.
- **Biometric data never reaches Pillsner.** Your fingerprint or face is handled entirely by Android;
  the app only asks the system whether the check succeeded and is told yes or no.
- While the lock is on, Pillsner hides its contents from screenshots and from the recent-apps
  thumbnail.
- Changing your PIN, turning biometric unlock off and turning the lock off each require you to
  confirm it is you first.

## 8. Logging

Pillsner writes no medication names or dosages to the Android log at information level or above in
release builds. It keeps no log file of its own, and no log is transmitted anywhere.

## 9. Your data, and how to see, correct or remove it

Because everything is on your device, you exercise your rights directly in the app and in Android,
not by writing to anyone.

- **See it.** Everything Pillsner holds is visible in the app: your medicines and their schedules in
  the medicine overview, and what you took, skipped or missed in the daily and history views.
- **Correct it.** Open a medicine to change its name, its dose, its dates, who prescribed it and its
  schedules. Editing a medicine does not rewrite history: doses you already answered keep the name
  and amount they were taken under.
- **Stop using a medicine.** Deactivating a medicine stops its reminders. Pillsner never deletes a
  medicine on its own, because an intake record that loses the medicine it belonged to stops being a
  record of anything.
- **Remove everything.** To erase all of Pillsner's data, either clear the app's storage in
  **Settings → Apps → Pillsner → Storage → Clear storage**, or uninstall the app. Both remove the
  database, the settings and the app lock material permanently. If you have the watch app installed,
  uninstall that too.
- **Remove it from a backup.** If Android has backed the app's data up to your Google account, that
  copy is removed by deleting it from your Google account backup, which is done in your Google
  account settings rather than in Pillsner.

There is no export feature at present. If one is ever added, it will write a file to a location you
choose on your own device, and this statement will say so.

## 10. Children

Pillsner is intended for adults managing their own medication, or a carer managing someone's
medication on that person's behalf. It is not directed at children and is not designed to appeal to
them. It collects nothing from anyone, of any age.

## 11. Security, stated honestly

Pillsner's data sits in the app's private storage, which Android keeps from other apps, and on a
modern Android device that storage is encrypted by the system while the device is locked. The
optional app lock adds a second barrier.

What that does not protect against: someone who has your unlocked phone and knows your PIN, a device
whose system security has been deliberately circumvented, or a backup you have chosen to keep
somewhere you do not control. No app can protect you from those. Keeping a screen lock on your phone
is the single most useful thing you can do for the privacy of everything on it, Pillsner included.

## 12. What this statement is not

This is a statement about privacy, not about medicine. Pillsner is a reminder, not a medical device,
and it gives no medical advice. What the app is and is not, and what it cannot promise about a
reminder arriving, is set out in the **Disclaimer** and the **Terms of Service**, both of which you
accept in the app before adding your first medicine and can read at any time in **Settings → Legal**.

## 13. Changes to this statement

This statement carries a version number and a date. If Pillsner ever changes in a way that affects
it — a new permission, a new way data moves, or anything that would leave the device — this statement
changes in the same release, and the Google Play Data safety declaration changes with it. Any such
change goes through the project's change process before it ships, so the reasoning is recorded
alongside the decision.

Earlier versions of this statement remain in the repository's history.

## 14. Contact

Questions about this statement, or about how Pillsner handles data:

- Open an issue at <https://github.com/hexmasternl/pillsner/issues>.
- Or write to the support address published on the Pillsner listing on Google Play.

Questions about your medication, your dose or your schedule are for your doctor or pharmacist. They
know your situation; the app does not.

---

Copyright (c) 2026 HexMaster. Pillsner is released under the MIT License; see [LICENSE](LICENSE).
