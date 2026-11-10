# Design

## Context

See proposal.md for the problem. The agenda is a Wear Compose M3 (1.6.2) `TransformingLazyColumn` inside a `ScreenScaffold`, and it uses the scaffold's default `contentPadding` (`UpcomingDosesScreen.kt`, around lines 65–68). In this version that padding leaves the extra room at the top and bottom of a list to items that declare `Modifier.minimumVerticalContentPadding(...)`. Only the empty-state item declares it (the #107 fix). `DoseCard` is a custom `Row` with a `MaterialTheme.shapes.large` background, not a Material `Card`, so it declares nothing, and the last card gets only the scaffold's small default bottom padding. By geometry the card needs about 30 px (about 15 dp) more room at the bottom on a 384 px round screen.

This cause is inferred from triage and has not been proven by a fix. Task 2.2 confirms it.

The failing tests from triage are already on `feature/fix-watch-agenda-bottom-inset` (`f56aa53`), in the worktree `../pillsner-worktrees/fix-watch-agenda-bottom-inset`:
- `RoundScreenFitTest#theLastDoseOfALongAgendaClearsTheBezel`
- `RoundScreenFitTest#theLastDoseOfALongAgendaClearsTheBezelAtTheLargestFont`

After triage, the product owner asked for the agenda to end with a small logo and the installed version, centred and completely on the screen. It belongs in this change because it becomes the list's last item: it decides how much room the end of the list needs, and it changes what "the last dose card scrolled to the end" looks like.

## Goals / Non-Goals

**Goals:**
- The agenda ends with the product mark and the version name, centred, on every state of the screen (doses, empty, phone not connected, never synced).
- Scrolled to the end, the mark and the version line fit inside the circle at font scale 1.0 and 1.24, in all six languages.
- The last dose card's whole outline fits inside the circle at the end of the list, at font scale 1.0 and 1.24.
- The same holds for the end of the details screen if it clips there too.
- The room at the end comes from the Wear scaffold's own padding mechanism, as `wearable-app` requires ("leave inset and round-screen padding to the Wear Compose scaffolds").

**Non-Goals:**
- The mark or the version on the details screen, or an About screen on the watch.
- Showing the version code, the application id or anything else from the phone's About screen.
- Making the mark tappable. The watch stays read-only with exactly two screens.
- Narrowing the cards, or changing their shape, colour or content.
- Changing the top of the list, which #107 already fixed and tested.
- #105 (the watch agenda live-update test that times out on CI). It's a separate issue.

## Decisions

**D1. The mark and version are one last item of the agenda's column: the mark above the version line, both centred.** A column, not a row, because the screen is narrowest where this item sits and a stacked item is about half as wide. It is added after the connection footer, in the empty state as well as with doses, so every state ends the same way. In the empty state the column is centred vertically as a group, so the mark sits directly under the empty text (and the footer) rather than pinned to the bottom edge; it only reaches "the bottom" by scrolling when the content is taller than the screen.
*Alternative:* pin it to the bottom of the screen outside the list. Rejected: it would cover dose cards on a screen this small, and the bottom of a round screen is the narrowest place to keep something permanently.

**D2. The mark is the phone's placeholder product mark, 24 dp.** `ic_pillsner_logo.xml` is copied into `src/wear/src/main/res/drawable` (with the three brand colours it references, if the watch's `colors.xml` lacks them), since a vector drawable cannot be shared through `src/shared`, which holds only the wire format. Its size is a new `WearDimens.markSize` token of 24 dp, one step above `iconSize`, so the capsule is still recognisable. It is decorative: `contentDescription = null`, because the version line next to it carries the meaning (design system §7).
*Note:* when the final artwork replaces the placeholder, both copies are replaced. The drawable's header comment says so.

**D3. The version line reads "Version 0.1.0": the version name only, from `BuildConfig`.** A new watch string `app_version` (`Version %1$s`) in six locales, rendered in `bodySmall` and `onSurfaceVariant` like the connection footer, so it stays quiet. The version name comes from `BuildConfig.VERSION_NAME` (the watch module turns on `buildFeatures.buildConfig`), the same source `app-about` requires on the phone, so it cannot disagree with the installed package. `MainActivity` passes it to `UpcomingDosesScreen` as a plain `String` parameter; it is not part of `WatchUiState`, because it never changes while the app runs. The version code is left out: it is the phone's number times ten plus one, which means nothing to a user and costs width.
*Alternative:* read it from `PackageManager`. Rejected: it needs a `Context` lookup and a nullable result for a value the build already knows.

**D4. The new last item declares the end-of-list padding; dose rows declare it only if the tests still need it.** The item gets `minimumVerticalContentPadding(...)` so the scaffold leaves it enough room to clear the bezel, and its text keeps the `roundTextInset()` side padding the other plain text uses. With an item under it, the last dose card can now scroll up to where the circle is wider, which is expected to make the two triage tests pass without touching `DoseCard`. If they still fail, dose rows declare `minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding)`, the value Wear M3 gives its own `Card` at the end of a list.
*Alternative:* add a fixed extra bottom padding to `contentPadding`. Rejected: it hard-codes a value the scaffold already works out per screen shape, and the spec leaves round-screen padding to the scaffold.
*Fallback:* if the scaffold's value turns out not to be enough for the mark and version on the small round screen at font scale 1.24, add the shortfall as a `WearDimens` token for the list's end padding, sized as a fraction of the screen like `textInsetFraction`. Record that here if it happens.

**D5. The triage tests stay as written, and the new item is measured the same way.** The triage tests measure the card's real outline (`MaterialTheme.shapes.large`) once the list is scrolled to the end; the fix must make them pass without changing what they assert. New cases scroll to the end and check that the mark's bounding box (all four corners, which is stricter than its circle) and every line of the version text lie inside the circle, at font scale 1.0 and 1.24, with doses and in the empty state with the never-synced footer, and at 1.24 in each of the six languages.

**D6. Check the details screen's end with the same kind of test.** It uses the same scaffold and column. If its last line already fits, the new case passes at once, and the screen's code stays as it is.

## Risks / Trade-offs

- [The empty state at the largest font, with the footer, is already taller than the screen; one more item makes it taller] → the existing "Round screen at the largest font" cases run unchanged and must still pass; the new item is at the end, so it is covered by "once scrolled to the end".
- [In the empty state the mark is not at the bottom edge but under the centred text] → accepted (D1); it is still the last thing on the screen and fully visible without scrolling at the default font.
- [The inferred cause for the dose card is wrong and the extra item changes nothing] → task 2.2 runs the triage tests right after the item is added; if they still fail, D4's dose-row padding, then its fallback.
- [More space at the end of the list on a square watch] → check the square preview, and accept a small extra gap.
- [The scroll transformation scales the last item, so the measured outline differs from the laid-out one] → the tests map bounds through each node's drawn position, as `RoundScreenFitTest` did for #107.
- [Two copies of the placeholder mark drift apart] → both are replaced together when the final artwork lands (D2).
