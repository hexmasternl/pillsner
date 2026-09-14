# Manual test cases

## MT-1 TalkBack reaches the action without a swipe

**Setup.** TalkBack on, the Medicines screen with at least one active and one inactive medicine.

**Steps.** Focus an active tile and open the actions menu. Then do the same on an inactive tile.

**Expect.**
- The active tile offers exactly one custom action, "Deactivate"; the inactive tile offers
  "Activate".
- Performing it moves the tile to the other section, and the announcement of an inactive tile still
  ends with "Inactive".

## MT-2 Largest font scale

**Setup.** System font scale at maximum.

**Steps.** Swipe a tile with a long medicine name open.

**Expect.** The revealed label reads in full, wrapping if it must; nothing on the tile is clipped;
the action is at least a comfortable thumb's width.

## MT-3 One-handed use

**Steps.** Holding the phone in one hand, swipe a tile in the active section open and tap the
button. Repeat in the inactive section.

**Expect.** Both are reachable with the thumb, and the list does not scroll while swiping sideways.

## MT-4 The reminder consequence

**Setup.** A medicine with a dose due later today, showing on the Home screen.

**Steps.** Deactivate it from the Medicines screen, then look at Home. Activate it again and look
again.

**Expect.** The dose disappears from Home when the medicine is deactivated and comes back when it is
activated. A reminder already showing for that medicine stays, as the alarm change intends: the
user can still take, skip or let it lapse.
