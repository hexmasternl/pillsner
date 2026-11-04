# Design

## Context

See proposal.md for the problem. The agenda is a Wear Compose M3 (1.6.2) `TransformingLazyColumn` inside a `ScreenScaffold`, and it uses the scaffold's default `contentPadding` (`UpcomingDosesScreen.kt`, around lines 65–68). In this version that padding leaves the extra room at the top and bottom of a list to items that declare `Modifier.minimumVerticalContentPadding(...)`. Only the empty-state item declares it (the #107 fix). `DoseCard` is a custom `Row` with a `MaterialTheme.shapes.large` background, not a Material `Card`, so it declares nothing, and the last card gets only the scaffold's small default bottom padding. By geometry the card needs about 30 px (about 15 dp) more room at the bottom on a 384 px round screen.

This cause is inferred from triage and has not been proven by a fix. Task 2.1 confirms it.

The failing tests from triage are already on `feature/fix-watch-agenda-bottom-inset` (`f56aa53`), in the worktree `../pillsner-worktrees/fix-watch-agenda-bottom-inset`:
- `RoundScreenFitTest#theLastDoseOfALongAgendaClearsTheBezel`
- `RoundScreenFitTest#theLastDoseOfALongAgendaClearsTheBezelAtTheLargestFont`

## Goals / Non-Goals

**Goals:**
- The last dose card's whole outline fits inside the circle at the end of the list, at font scale 1.0 and 1.24.
- The same holds for the end of the details screen if it clips there too.
- The fix uses the Wear scaffold's own padding mechanism, as `wearable-app` requires ("leave inset and round-screen padding to the Wear Compose scaffolds").

**Non-Goals:**
- Narrowing the cards, or changing their shape, colour or content.
- Changing the top of the list or the empty and footer states, which #107 already fixed and tested.
- #105 (the watch agenda live-update test that times out on CI). It's a separate issue.

## Decisions

**D1. Dose rows declare `minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding)`.** This is the value Wear M3 gives its own `Card` when it is the first or last item of a list, so a custom card ends up spaced like a Material card. The scaffold then grows the bottom padding only when a card is actually last. The day and time headings keep their own (`ListHeaderDefaults`) behaviour.
*Alternative:* add a fixed extra bottom padding to `contentPadding`. That's rejected: it hard-codes a value the scaffold already works out per screen shape, it would add space on square watches too, and the spec leaves round-screen padding to the scaffold.
*Fallback:* if the M3 value turns out not to be enough on the small round screen, add the shortfall as a `WearDimens` token for the list's end padding, sized as a fraction of the screen like `textInsetFraction`. Record that here if it happens.

**D2. The tests stay as triage wrote them.** They measure the card's real outline (`MaterialTheme.shapes.large`) once the list is scrolled to the end. The fix must make them pass without changing what they assert.

**D3. Check the details screen's end with the same kind of test.** It uses the same scaffold and column. If its last line already fits, the new case passes at once, and the screen's code stays as it is.

## Risks / Trade-offs

- [The inferred cause is wrong and the M3 padding changes nothing] → task 2.1 runs the tests right after D1; if they still fail, use the D1 fallback and update this design.
- [More bottom space on a square watch] → `minimumVerticalContentPadding` is applied per item by the scaffold. Check the square preview, and accept a small extra gap at the end of the list.
- [The scroll transformation scales the last item, so the measured outline differs from the laid-out one] → the triage tests already map bounds through each node's drawn position, as `RoundScreenFitTest` did for #107.
