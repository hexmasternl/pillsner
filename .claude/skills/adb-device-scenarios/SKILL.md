---
name: adb-device-scenarios
description: Scripted emulator scenarios for Pillsner's fragile reminder paths - reboot, time zone moves, daylight-saving transitions, manual clock changes, Doze, process death and notification permission - driven with adb and checked through dumpsys alarm and dumpsys notification. Use when a change touches reminder scheduling, alarms, notifications or the boot/time receivers; when a tasks.md item says "manual test" for any of these; and when reproducing a bug report about a reminder that didn't arrive or arrived at the wrong time.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

`CLAUDE.md` requires reboot, time zone changes, daylight-saving transitions and system clock changes
to be covered by a test or a documented manual test. Most of these can be driven on the emulator
from the command line, which turns a manual test into one the agent runs and reports. Use this skill
for those, and keep "manual" for what truly needs a person (a real watch, a real lock screen
experience, a specific phone maker's battery manager).

**Emulator only.** These commands change the device's clock, time zone and power state. Never run
them against a physical device unless the user explicitly says that device is a test device.

The app id is `nl.hexmaster.pillsner` (debug and release share it). Get the emulator running with the
`emulator-tests` skill, step 1, and install the debug build: `./gradlew :app:installDebug` from `src/`.

## Before every scenario

1. Grant notifications so reminders can show:
   `adb shell pm grant nl.hexmaster.pillsner android.permission.POST_NOTIFICATIONS`
2. Set up a medicine with a schedule in the app whose next dose is a known time (drive the UI with
   `adb shell input` or a short Compose test, or set it up by hand once and keep the emulator
   snapshot). Use a made-up medicine name.
3. Record the starting state:
   - `adb shell dumpsys alarm | grep -A6 nl.hexmaster.pillsner` — the armed alarms and their
     trigger times. Reminder alarms are alarm-clock alarms (`setAlarmClock`), so they also show in
     `adb shell dumpsys alarm | grep -A3 "next alarm clock"`.
   - `adb shell date` and `adb shell getprop persist.sys.timezone`.

## Scenarios

### Reboot

```bash
adb reboot
adb wait-for-device shell 'while [ -z "$(getprop sys.boot_completed)" ]; do sleep 1; done'
adb shell dumpsys alarm | grep -A6 nl.hexmaster.pillsner
```

**Pass:** the same reminder alarm is armed again for the same moment, without opening the app (the
boot receiver re-arms it). Also check `adb logcat -d | grep -i "FATAL\|ANR" | grep -i pillsner` is
empty — the reboot wake has crashed before (`fix-boot-wake-service-crash`).

### Time zone move

```bash
adb shell settings put global auto_time_zone 0
adb shell cmd alarm set-timezone America/New_York
adb shell getprop persist.sys.timezone   # confirm it took
adb shell dumpsys alarm | grep -A6 nl.hexmaster.pillsner
```

**Pass:** the reminder is re-armed for what the schedule defines in the new zone (a dose "at 08:00"
fires at 08:00 New York time, per the `reminder-scheduling` spec — check the spec for the agreed
behaviour rather than assuming).

### Daylight-saving transition

Pick a zone with DST and jump to just before a transition, with a dose scheduled inside the hour that
shifts:

```bash
adb shell settings put global auto_time 0
adb shell settings put global auto_time_zone 0
adb shell cmd alarm set-timezone Europe/Amsterdam
# 2026 transitions in Europe: spring forward 29 March 01:00 UTC, fall back 25 October 01:00 UTC.
adb shell cmd alarm set-time <epoch-millis a few minutes before the transition>
adb shell date
```

Check both directions: a dose at 02:30 local on the spring-forward day (that time doesn't exist) and
a dose at 02:30 local on the fall-back day (that time happens twice). **Pass:** what the
`reminder-scheduling` spec says happens — one reminder, at the agreed moment, not zero and not two.
Let the clock run past the dose and confirm with `adb shell dumpsys notification --noredact | grep -A4
nl.hexmaster.pillsner`.

### Manual clock change

```bash
adb shell settings put global auto_time 0
adb shell cmd alarm set-time <epoch-millis>   # forward past a due dose, then separately backwards
```

**Pass forwards:** a dose whose moment was jumped over is handled the way the
`reminder-delivery-resilience` spec says (reminded late or shown as needing an answer — not silently
lost). **Pass backwards:** no duplicate reminder for a dose already answered.

### Firing a reminder now

There's no command to fire an app's alarm. Set the clock to one minute before the dose
(`cmd alarm set-time`) and wait, or schedule a dose for two minutes from now. Then check
`dumpsys notification` as above, and answer it from the notification shade with
`adb shell cmd statusbar expand-notifications` plus a UI step if the scenario needs an answer.

### Doze and standby

```bash
adb shell dumpsys deviceidle force-idle      # deep Doze now
# ...let the dose come due...
adb shell dumpsys deviceidle unforce
adb shell am set-inactive nl.hexmaster.pillsner true   # app standby
```

**Pass:** the reminder still arrives on time — alarm-clock alarms are exempt from Doze.

### Process death

`adb shell am kill nl.hexmaster.pillsner` (only kills a backgrounded app; send it to the background
with `adb shell input keyevent KEYCODE_HOME` first). **Pass:** the next reminder still fires.
Don't use `am force-stop` for this — force-stop cancels all of the app's alarms by design and is a
different scenario (the user deliberately stopping the app), only to be tested when a spec covers it.

## After every scenario

Restore the emulator, so the next scenario and the next test run start from normal time:

```bash
adb shell settings put global auto_time 1
adb shell settings put global auto_time_zone 1
adb shell dumpsys deviceidle unforce
adb shell am set-inactive nl.hexmaster.pillsner false
```

If `cmd alarm set-time` or `set-timezone` reports an error on this emulator image, say so and treat
that scenario as **not run** — don't substitute a different mechanism silently.

## Reporting

Per scenario: what was set up, the commands, what `dumpsys` showed before and after, pass or fail
against the named spec requirement. Passed scenarios go in the change's `## Verification` section
(`verify-change` step 6) and come off the pull request's manual-testing list. A failure is a bug in
this change; fix it before the pull request.
