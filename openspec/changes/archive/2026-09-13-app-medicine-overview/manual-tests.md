# Manual test cases

These cannot be asserted automatically and are run by hand on a device before a release.

## MT-1 Largest font scale with both sections

**Setup.** System font scale at its maximum. Five active and three inactive medicines, covering
every schedule shape, with at least one long medicine name.

**Steps.** Open Medicines. Scroll from the top of the list to the bottom.

**Expect.**
- The screen title, both section headers and all eight tiles are reachable by scrolling; the
  screen scrolls as one list rather than two.
- No text is clipped or ellipsised on any tile or header; long names wrap.
- The add button stays visible over the list the whole way down.
- At the very bottom the last tile is fully visible and is not covered by the add button.

## MT-2 TalkBack

**Setup.** TalkBack on. The same eight medicines.

**Steps.** Swipe through the list from the title.

**Expect.**
- The title and both section headers are announced as headings.
- Each tile is announced as a single item holding the medicine name and its schedule description,
  for example "Ibuprofen, Twice a day".
- Every tile under Inactive additionally announces "Inactive".
- The add button is announced with the label "Add medicine".
