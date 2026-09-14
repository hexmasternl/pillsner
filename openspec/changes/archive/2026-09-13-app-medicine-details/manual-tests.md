# Manual test cases

## MT-1 Largest font scale on the details screen

**Setup.** System font scale at maximum. A medicine with three schedules.

**Steps.** Open it from its tile and scroll from the name field to the Save button.

**Expect.** Every field, the Active row with its switch, all three schedule rows, "Add schedule"
and Save are reachable by scrolling; nothing is clipped; the supporting text under Active wraps
rather than truncating.

## MT-2 TalkBack on the details screen and the tile

**Setup.** TalkBack on.

**Expect.**
- On the Medicines screen, a tile's default action (double tap) is announced as "Open medicine
  details" and opens the medicine; "Deactivate" or "Activate" is still listed in the actions menu.
- On the details screen, the Active row announces its label, its supporting text and whether the
  switch is on; toggling it is announced.

## MT-3 Editing keeps history, end to end

**Setup.** A medicine with a dose already recorded as taken and a dose planned for later today.

**Steps.** Rename the medicine and change its default dose. Save. Then look at the Home screen and,
if an intake history is available, at the recorded dose.

**Expect.** The recorded dose still reads with the old name and amount. The dose still ahead
reflects the new ones. A reminder already on the notification shade is left as it is.

*Automated coverage:* `MedicationDaoTest` proves the dose rows are untouched by the update, and the
reminder layer's own refresh rule is covered by `RefreshPlannedDosesTest`. Only the end-to-end view
of both together is left to this manual case.

## MT-4 Process death with a half-edited form

**Steps.** Open a medicine, change its name, add a schedule. Enable "Don't keep activities" in
developer options, put the app in the background and return.

**Expect.** The form comes back in details mode with the edited name and the new schedule, and back
still asks to discard.
