# Manual tests: app-legal-information

Two things in this change cannot be proved by an automated test on a build machine: how the
acceptance screen behaves under a real screen reader, and that the app is genuinely reachable with
the radios off. Both need a person with a device.

Set up once: install the debug build on a phone and clear its data, so no acceptance is stored and
the gate appears on the first medicine.

## 1. The gate at the largest font scale, in dark theme

1. Settings → Display → set the font size to its largest step, and the theme to dark.
2. Open Pillsner → Medicines → the add button.
3. **Expect:** the whole disclaimer is reachable by scrolling; no line is clipped, cut off or
   ellipsised, and no text runs off either edge.
4. **Expect:** the accept button stays pinned below the text, fully visible, at least a thumb tall,
   and never scrolls out of reach.
5. **Expect:** the disclaimer, the hint and the button all read clearly against the dark surface —
   nothing is grey-on-grey.

## 2. The gate with TalkBack

1. Settings → Accessibility → turn TalkBack on.
2. Open Pillsner → Medicines → the add button.
3. Swipe by heading (or use the heading navigation gesture).
   **Expect:** each of the disclaimer's five section headings is reached in order.
4. Focus the accept button before scrolling.
   **Expect:** it announces its label, that it is disabled, and that the disclaimer must be read to
   the end first.
5. Scroll to the end of the disclaimer without moving focus back to the button.
   **Expect:** the change is announced ("You can now accept") without the user going looking for it.
6. Activate the accept button.
   **Expect:** the add-medicine form opens and announces itself.
7. Press back on the untouched form.
   **Expect:** Medicines, with the Medicines navigation item selected — not the acceptance screen.

## 3. Both documents with no network at all

1. Turn on aeroplane mode and confirm Wi-Fi and mobile data are both off.
2. Clear the app's data so nothing is accepted.
3. Open Pillsner → Settings → Legal → Disclaimer, then back → Terms of Service.
   **Expect:** both documents are shown in full, with their version and effective date, and nothing
   is blank, spinning or reporting an error.
4. Back to Medicines → the add button → read to the end → accept.
   **Expect:** acceptance is given and the form opens, with no network at any point.
5. Settings → Legal.
   **Expect:** the status line now names today's date.

## 4. Dutch

1. Settings → Language → Nederlands, then restart Pillsner.
2. Settings → Juridisch → Disclaimer and Gebruiksvoorwaarden.
   **Expect:** both documents are entirely in Dutch; no English sentence anywhere.
   **Expect:** "Pillsner" and "Eduard Keilholz" are still spelled that way.
   **Expect:** the acceptance date and the effective date read the Dutch way (14 september 2026).
