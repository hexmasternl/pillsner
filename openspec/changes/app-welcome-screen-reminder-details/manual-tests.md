# Manual tests

One pass on a device, covering the things that need a real notification shade, a real rotation and
a real hour to pass (task 9.4).

## 1. Answering from the screen takes the notification with it

1. Add a medicine with a dose a few minutes out and wait for the reminder to arrive.
2. With the notification showing, open the app and tap the dose on Home.
3. Tap **I took it**.

**Expected:** the screen closes back to Home, the tile is gone, and the notification has gone from
the shade without being swiped. The next reminder that arrives is for the next dose, not this one.

## 2. Answering from the shade while the screen is open

1. Open a pending dose from Home and leave the detail screen in front of you.
2. Pull down the shade and answer the same dose from its notification.

**Expected:** the detail screen switches to the settled state on its own — the three answers are
replaced by a line saying what was recorded, and Close is the only control left.

## 3. Early and late both warn and neither blocks

1. Open a dose more than an hour before it is due.
2. Open a dose more than an hour after it was due.

**Expected:** the first shows the blue "not due yet" banner, the second the red "overdue" one, and
in both cases all three answer buttons are live and recording works.

## 4. The warning changes on its own

1. Open a dose about an hour and five minutes before it is due and leave the screen open.

**Expected:** at an hour before, the "not due yet" banner disappears by itself, with no tap and no
other change to the dose.

## 5. Rotation and process death

1. Open a dose, rotate the phone, and confirm the same dose is still shown.
2. With the screen open, kill the process from Android Studio or with `adb shell am kill`, then
   reopen the app from recents.

**Expected:** the same dose comes back both times. The dose id travels in the route, so nothing has
to be rebuilt.

## Status

Not yet run. Every part of this that can be proved without a device is covered by the automated
suite: the timing rule, the single answer path, the four screen states, the tile's tap target and
the navigation in and out of the destination.
