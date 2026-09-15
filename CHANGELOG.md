# Changelog

Release notes for Pillsner, newest first. The short store-listing version of each release lives in
`distribution/whatsnew/`, one file per language, and is limited to 500 characters by Google Play.
This file is the full account.

## 1.0.0 — 15 September 2026

The first release of Pillsner: a medication reminder for Android, with a companion app for Wear OS.
It reminds you when a dose is due, lets you answer in one tap, keeps a record of what you did, and
stores all of it on your own device. Nothing is sent anywhere.

**Requirements**

| | |
| --- | --- |
| Phone | Android 8.0 (API 26) or newer |
| Watch (optional) | Wear OS 3 (API 30) or newer, paired with the phone |
| Languages | English and Dutch |

### Medicines

- **Add a medicine** with its name, a default dose and unit (mg, g, mcg, ml, tablet, capsule, drop,
  puff or unit), the dates you take it between, and who prescribed it.
- **Give a medicine as many schedules as it needs**, each with its own amount. Three schedule shapes
  are supported: one or more clock times every N days, one or more clock times on chosen weekdays,
  and one dose every N hours starting from a first dose time. Every schedule shows a plain-language
  description of itself, and the editor previews the description while you build it.
- **The Medicines screen** lists what you take, split into active and inactive, sorted by name, each
  tile carrying the schedule in words. Swipe a tile sideways, or use the button that appears, to
  deactivate or reactivate a medicine.
- **Open a medicine to change anything**: name, dose, dates, prescriber, schedules and whether you
  are currently taking it. The form remembers a half-finished edit through rotation.
- **A medicine is never deleted.** Stopping one deactivates it; the medicine, its schedules and every
  dose it ever produced stay on the device. Editing a medicine never rewrites the past: doses you have
  already answered keep the name and amount they were taken under, and only what lies ahead follows
  the change.

### Reminders

- **Exact, on the minute.** Reminders are set as real alarms, the one kind Android and phone makers
  do not defer, so a dose due at 08:00 is announced at 08:00, including when the phone is dozing. A
  scheduled medicine therefore puts the alarm icon in your status bar; that is the honest consequence
  of having set an alarm.
- **Three answers, one tap, without opening the app**: *I took it*, *Not yet* (snoozes for
  15 minutes) and *Not going to* (records a skip). Swiping the notification away counts as *Not yet*.
- **An unanswered reminder asks again** every 15 minutes, at most four times, and never past the
  moment the dose lapses. Any answer stops it at once.
- **A dose you never answer becomes missed** when the next dose of the same medicine is due, or after
  24 hours, whichever comes first. A skipped dose is a skipped dose; it never turns into a missed one.
- **Several doses due at once** each get their own notification with their own actions, grouped under
  a summary that says how many medicines are due.
- **A due dose presents itself on a locked or busy phone** rather than waiting silently in the shade.
  On Android 14 and newer this uses the full-screen reminder privilege; where the system does not
  grant it, the reminder falls back to a heads-up notification.
- **Reminders survive** a reboot, an app update, the clock being changed, a move to another time zone
  and the daylight-saving switch. A dose that was already announced is never announced a second time
  because the clock moved.
- **A background watchdog** periodically checks that every expected alarm is still armed and re-arms
  any the platform dropped. It is a safety net, not the delivery mechanism; on a healthy phone it
  finds nothing to do.
- **Pillsner never opens a system dialog you did not ask for.** It does not request a
  battery-optimisation exemption. If a dose ever lapses without a reminder having been shown, the
  Home screen says so, with a button that takes you to the right background-settings screen: the
  phone maker's own auto-start page where there is one, Android's battery-optimisation list otherwise.
- **On the lock screen** a reminder shows only "Time for your medicine", never the name or the amount,
  whenever the phone is set to hide sensitive notifications.

### Home and dose detail

- **The Home screen** shows your next doses, soonest first, up to five at a time. A dose whose time
  has passed and that you have not answered stays at the top, because it is still to be taken.
  Answering a dose from the notification removes its tile without leaving the screen.
- **A readiness banner** appears above the list only when reminders cannot be delivered as designed:
  notifications are turned off, exact alarms are not permitted, or a reminder was observed to have
  been missed. Each banner has a button that opens the exact system setting involved. When everything
  is in order there is no banner.
- **Tap a dose to open its detail**, with the same three answers the reminder offers. Answering more
  than an hour early or more than an hour late shows an advisory warning; the answer is still recorded
  exactly as given.

### Usage history

- **Every medicine has a usage history**, reached from its details screen, over the last week, month
  or three months.
- It shows **adherence as a whole percentage**, the number of doses scheduled and taken, and a
  breakdown of taken, skipped, unanswered and missed doses.
- **A bar chart** shows the period as one bar per day (one week) or per week (one and three months),
  each stacked from the four outcomes in the same colours as the breakdown, and readable by a screen
  reader.

### On your wrist

- **A Wear OS companion app** shows what you have to take in the next six hours, soonest first, with
  the name, the amount and the time. An overdue, unanswered dose stays at the top.
- **It tells you when there is nothing**, and when it cannot reach the phone, so a stale list is never
  mistaken for a live one.
- **The watch app only shows.** Answering a dose happens where it always did: on the reminder
  notification, which also appears on the watch with the same three actions, or in the phone app.
- **It reads in the language the phone app is set to**, not the watch's own.

### App lock

- **An optional app lock** protects Pillsner with a PIN and, once set up, fingerprint or face unlock.
  It is off by default.
- The app locks on a cold start and again whenever it returns to the foreground. Five wrong PINs in a
  row start a 30-second cooldown that doubles with each further five, up to five minutes, and
  survives restarting the app.
- **Screenshots and the recent-apps thumbnail are hidden** while the lock is on.
- **Changing the PIN, turning biometrics off and turning the lock off** each ask you to prove it is
  you first. Each confirmation is good for that one change.
- The PIN is stored only as a device-bound verifier, never as the PIN itself.

### Settings

- **Language**: follow the phone, or choose English or Dutch. Everything follows the choice, including
  dates, times, weekday names, decimal separators, sort order and the reminder that arrives while the
  app is closed. A change applies on the next start, and Settings says so until then.
- **Theme**: system default, light or dark. Applies immediately and before the first frame on the next
  start. Material You dynamic colour is deliberately off, so the app always looks like Pillsner.
- **Disclaimer and Terms of Service**: shown once, before you add your first medicine, and you are
  asked to accept both. They stay readable from Settings together with the date you accepted them.
  They are versioned; a revised document asks again before your next medicine, but never blocks what
  you already have.
- **About**: the app name and what it means, the version, the application id and the author.
- **Reset app**, in a danger zone at the very end of Settings: erases every medicine, schedule and
  intake record from the device and cancels every pending alarm, notification and watch payload. It
  takes a tap, a ticked acknowledgement and a second tap. Your language, theme, app lock and accepted
  documents survive; the app stays configured and is simply empty. There is no undo.

### Privacy and permissions

- **Everything stays on the device.** There is no account, no cloud sync, no analytics, no advertising
  and no crash reporting. Neither the phone app nor the watch app declares the `INTERNET` permission,
  so neither can send anything to a server. The full statement is in `PRIVACY.md`.
- **The one exception is your own watch.** With the companion app installed, the names, amounts and
  times of the doses you still have to take travel over the direct link between your paired phone and
  watch, through Google Play services. Nothing goes to a server. Uninstalling the watch app removes
  them.
- **Permissions asked for**: post notifications, schedule exact alarms, run again after a reboot, a
  short foreground service so an alarm can open the database and work out what is due, and the
  full-screen reminder privilege. Nothing else: no location, no contacts, no network, and no
  battery-optimisation exemption.
- **Medication names and amounts are never written to logs** in a release build.

### Not in this release

- Stock tracking and refill warnings. The README lists them as part of the intended scope; they are
  not built yet.
- Exporting or sharing history with a caregiver.
- Answering a dose from the watch app itself. The watch notification can be answered; the list cannot.
- Languages other than English and Dutch.
- If Android's own backup is switched on for your Google account, Android may include Pillsner's
  data in that backup. Pillsner does not control this. See section 6 of `PRIVACY.md`.

### For developers

- Kotlin, Jetpack Compose with Material 3, single activity, MVVM with unidirectional data flow, Room
  for persistence, `AlarmManager` alarm-clock alarms for delivery and WorkManager only for the
  watchdog. Compose for Wear OS on the watch, with the Wearable Data Layer for sync.
- Toolchain: JDK 21, Gradle 9.7.1, AGP 9.4.0, Kotlin 2.4.20, Compose BOM 2026.09.00, compile and
  target SDK 37. Every version is pinned in `src/gradle/libs.versions.toml`.
- Behaviour is specified in `openspec/specs/`, and every feature above was delivered through an
  archived change under `openspec/changes/archive/`. Scheduling and domain logic are covered by unit
  tests, every schema version by a migration test, and the main flows by Compose UI tests.
- Builds `0.1.0` through `0.1.9` on GitHub were pipeline verification builds made while setting up the
  release workflow; they carry no release notes of their own and are superseded by this release.
