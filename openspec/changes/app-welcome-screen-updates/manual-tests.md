# Manual tests: app-welcome-screen-updates

Four checks on a real device (tasks 7.4 to 7.7). Each one is about a dose that already exists
reacting to a change the user makes elsewhere, which is where the withdraw-and-refresh rules either
hold or quietly do the wrong thing.

Set up once: install the debug build on a phone and clear its data.

## 1. Two medicines at the same time, one moved (task 7.4)

1. Add two medicines, both with a dose at the same time today.
2. Open the first and move its schedule to a different time.

**Expect:** the second medicine's dose stays exactly where it was, and the first medicine's old dose
is gone. Matching on the moment alone rather than on the medicine *and* the moment is what took the
wrong dose here before.

## 2. Renaming a medicine with a dose due today (task 7.5)

1. Add a medicine with a dose a few minutes out and wait for the tile to appear on Home.
2. Rename the medicine.

**Expect:** the Welcome tile shows the new name, and so does the reminder notification when it
arrives. A dose keeps a snapshot of its medicine's name so history stays readable, but while it is
still pending that snapshot has to follow the medicine.

## 3. Editing a schedule while its reminder is showing (task 7.6)

1. Let a reminder arrive and leave the notification on screen.
2. Edit the medicine so that dose is no longer planned.

**Expect:** the notification disappears. Leaving it up would offer three answers that resolve to a
dose which no longer exists.

## 4. A time zone change with a reminded dose outstanding (task 7.7)

1. Let a reminder arrive for a dose and do not answer it.
2. Change the phone's time zone.

**Expect:** the dose is neither withdrawn nor announced a second time. Only a change the *user* made
to a medicine may withdraw a dose they have already been told about; a clock or zone move may not.

## Status

Not yet run. All four need a physical device and a real time-zone change. The automated suite covers
the withdraw rules at the DAO and use-case level; these confirm they hold end to end.
