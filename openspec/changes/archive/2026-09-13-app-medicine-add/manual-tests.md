# Manual test cases

These cannot be asserted automatically and are run by hand on a device before a release.

## MT-1 Largest font scale on the form, keyboard open

**Setup.** System font scale at its maximum. The Add medicine form with three schedules in the draft.

**Steps.** Scroll from the name field to the bottom. Tap the name field so the keyboard opens, then
scroll again.

**Expect.**
- Every field, every schedule row, the "Add schedule" button and the Save button are reachable by
  scrolling, and nothing is clipped.
- With the keyboard open, the focused field stays visible and Save sits above the keyboard rather
  than behind it.

## MT-2 Largest font scale in the schedule editor

**Setup.** Maximum font scale. The editor on "Every N days" with four times in the list.

**Expect.** The amount field, the pattern selector, the interval stepper, all four time rows, the
"Add time" button, the preview card and the Done button are all reachable, and the pattern selector
wraps rather than truncating its labels.

## MT-3 TalkBack on the form

**Setup.** TalkBack on, an empty form, Save tapped so the errors are showing.

**Expect.**
- Each field announces its label; the name field also announces "Enter a name."
- The schedules header is announced as a heading.
- The remove button on a schedule row announces "Remove schedule".

## MT-4 TalkBack in the schedule editor

**Setup.** TalkBack on, the "Weekdays" pattern, Wednesday selected.

**Expect.** The Wednesday chip announces "Wednesday" and that it is selected; a time row announces
its time; the preview card is read as the description it shows.

## MT-5 Rotation and process death

**Setup.** A form with a name, a dose and one schedule.

**Steps.** Rotate the device. Then enable "Don't keep activities" in developer options, put the app
in the background, and return to it.

**Expect.** In both cases the form comes back with the name, the dose and the schedule row intact.

*Automated coverage:* `DraftSaverTest` covers the save-and-restore mechanism this relies on, at
every schedule shape; only the platform's own recreation is left to this manual case.
