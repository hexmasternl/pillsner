# Manual tests: app-wearable-support

Everything here needs two devices that are actually paired — a phone (or phone emulator) with the
Pillsner phone app and a Wear OS watch (or Wear emulator) with the watch app, both signed with the
same key. No automated test in this repository can stand in for that: the Wearable Data Layer is
unavailable on an unpaired phone, which is why the two Data Layer instrumented tests skip there.

Set up once: pair the two devices, install both apps, and add a medicine on the phone with a
schedule that produces a dose in the next hour and one about eight hours out.

## 1. The list appears

1. Open Pillsner on the phone, then open Pillsner on the watch.
2. **Expect:** "Next 6 hours" with the dose due in the next hour: name, amount and time.
3. **Expect:** the dose eight hours out is **not** listed.

## 2. A dose answered on the phone leaves the watch

1. With the watch screen open, take the dose on the phone — from the notification or the app.
2. **Expect:** within a few seconds the entry disappears from the watch.
3. **Expect:** with nothing else in the window, "No medicines scheduled for the upcoming 6 hours".

## 3. A dose whose time has passed stays, at the top

1. Let a dose become due and do not answer it.
2. Add a second medicine due in two hours.
3. **Expect:** the overdue one is first, in the red container with the error icon; the other is
   below it in the blue one.

## 4. The phone goes out of reach

1. With a list showing on the watch, turn Bluetooth off on the phone (or turn the phone off).
2. **Expect:** within a minute the footer reads "Phone not connected".
3. **Expect:** the list itself is still shown — it is the best answer available — and doses drop
   off it as their six hours pass, because the watch filters with its own clock.

## 5. Reopening offline

1. Leave the phone unreachable and close the watch app, then open it again.
2. **Expect:** the last list is still there, with the footer. Nothing is blank and nothing spins.

## 6. Never synced

1. On a watch that has never received a list (a fresh install, phone not reachable), open the app.
2. **Expect:** "Open Pillsner on your phone to sync".

## 7. The language follows the phone

1. Set the phone app to Nederlands in Settings and restart it as the app asks.
2. **Expect:** the watch shows "Komende 6 uur", Dutch amounts ("2 tabletten") and Dutch times.
3. Set it back to English and restart.
4. **Expect:** the watch follows again, with no action on the watch.

## 8. Notifications still bridge

1. With the watch app installed, wait for a dose to become due.
2. **Expect:** the phone's reminder notification appears on the watch with **I took it**,
   **Not yet** and **Not going to**.
3. Answer it from the watch.
4. **Expect:** the phone records the answer, and the dose leaves the watch list.

This is the one regression the watch app could cause and the reason the manifest sets no
notification bridge mode.

## 9. Round and square

1. Run the watch app on a large round, a small round and a square Wear device.
2. **Expect:** on all three the header, the cards and the footer are fully visible, the list
   scrolls with the rotary crown or bezel, and nothing is clipped at the edges.

## 10. Largest watch font

1. Set the watch's font size to its largest.
2. **Expect:** names and amounts wrap rather than truncate, cards stay at least 48 dp tall, and
   the empty-state sentence is readable in full.

## 11. A watch without the phone app

1. Uninstall the phone app, leaving the watch app installed.
2. **Expect:** the watch shows "Open Pillsner on your phone to sync". It never crashes and never
   shows a stale list as if it were live.

## 12. A phone without Play services

1. On a phone without Google Play services, use the app normally.
2. **Expect:** everything works exactly as before — reminders, intake, settings. Nothing about a
   watch is mentioned anywhere in the phone UI, and no error appears.
