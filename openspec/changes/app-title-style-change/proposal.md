## Why

The name *Pillsner* is a blend of **Pills** and Part**ner**, but the title on the welcome screen renders as one flat word, so nobody reading it learns where the name came from. Colouring the two source fragments differently tells that story in the one place every user looks first, and costs nothing in layout, size or reading order.

## What Changes

- The app title on the welcome screen is rendered as a single line of text in two colours: `Pills` in the secondary (Pillsner Blue) colour role and `ner` in the primary (Pillsner Green) colour role. It stays one visual word with no space, no gap and no change in size, weight, font or alignment.
- A reusable `PillsnerWordmark` composable is introduced so any later surface that shows the name (an About screen, a splash) renders it identically instead of repeating the split.
- The title continues to come from the `app_title` string resource; the split point is derived from that resource rather than from two hard-coded fragments, so the wordmark degrades to a single-colour title if the resource is ever changed.
- Screen readers, `testTag` lookups and text-matching tests continue to see one node whose text is "Pillsner". The colouring is decoration only and carries no meaning that is lost without it.
- The design system gains a short wordmark entry so the two-colour treatment is documented rather than folklore.

Not breaking. No behaviour, data, scheduling or permission changes.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `welcome-screen`: the "Welcome screen header" requirement gains the two-colour wordmark treatment for the title, and an explicit guarantee that the title is still exposed to accessibility services and tests as the single word "Pillsner".

## Impact

- `src/app/src/main/java/nl/hexmaster/pillsner/ui/home/WelcomeHeader.kt` — the only current caller; its `Text` is replaced by the new wordmark composable.
- New file under `src/app/src/main/java/nl/hexmaster/pillsner/ui/components/` — `PillsnerWordmark`.
- `docs/design-system.md` — a new subsection documenting the wordmark, plus the matching entry in `docs/design-system.html`.
- No new dependencies, no new strings, no manifest or Gradle changes. The wear and shared modules do not render the title and are untouched.
