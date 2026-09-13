# Manual test cases

## MT-1 A Dutch phone, nothing chosen

**Steps.** Set the phone to Dutch. Install Pillsner and open it without touching Settings.

**Expect.** Everything is Dutch: the navigation labels, the screens, the dates and the reminder.

## MT-2 A phone speaking neither language

**Steps.** Set the phone to German. Open Pillsner.

**Expect.** Everything is English, and Settings shows "System default".

## MT-3 Choosing Dutch on an English phone

**Steps.** With the phone in English, open Settings and pick Nederlands.

**Expect.** The notice "Restart Pillsner to apply the new language" appears at once and the screen
stays English. Force-stop the app and reopen it: everything is Dutch, and the notice is gone.

## MT-4 Following the phone

**Steps.** Leave the setting on System default. Change the phone's language from English to Dutch
while Pillsner is in the background, then return to it.

**Expect.** The app follows the phone.

## MT-5 A reminder in the chosen language

**Setup.** Dutch chosen and applied, a medicine due in a few minutes.

**Steps.** Lock the phone and wait for the reminder.

**Expect.** The notification reads "Neem 40 mg van je medicijn 'Ibuprofen', om 08:00", with the
actions Ingenomen, Nog niet and Sla ik over. This is the case an automated test cannot reach: it
arrives from a receiver with no activity behind it.

## MT-6 Dutch at the largest font scale

**Setup.** Dutch applied, system font scale at maximum.

**Steps.** Walk through Start, Medicijnen, a medicine's details, the schedule editor and
Instellingen.

**Expect.** Nothing is clipped or ellipsised. Dutch is longer than English, so the navigation
labels (Start, Medicijnen, Instellingen) deserve a second look: a label that does not fit is
reworded, never truncated.

## MT-7 Startup on a low-end device

**Setup.** The oldest supported device to hand.

**Steps.** Cold-start the app a few times and watch for a pause before the first frame.

**Expect.** No perceptible delay. The language is read from one small file, once, before anything
is drawn; if it is ever measurable, the read moves to a `SharedPreferences` mirror.
