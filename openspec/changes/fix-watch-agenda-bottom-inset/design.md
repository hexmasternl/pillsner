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

**D5. The triage tests keep their assertion; how they find the last card may change.** The triage tests measure the card's real outline (`MaterialTheme.shapes.large`) once the list is scrolled to the end, and that assertion (the whole outline inside the circle) must not be weakened. Their selection does change: today they take the click-action nodes in the lower half of the screen and require at least one, but with an item under it the last card may come to rest above the centre, where that selection would either find nothing or measure an earlier card. The tests instead pick the last dose card of the list, wherever it is drawn: the click-action node with the greatest bottom edge once scrolled to the end. Task 1.2 still runs them unchanged first, to see them fail for the reported reason; task 1.4 changes the selection and confirms they still fail on the unchanged screen code.

**D6. "Scrolled to the end" means the list's last item, and each state is named.** Every case that scrolls to the end scrolls to the last index of the column, not to a fixed index: the existing `everyLanguageFitsTheRoundScreenAtTheLargestFont` uses `performScrollToIndex(1)`, which stops being the end once the never-synced state has three items, so it is updated. The new cases check that the mark's bounding box (all four corners, which is stricter than its circle) and every line of the version text lie inside the circle, scrolled to the end, in these states:

| State | Font scale | Language |
| --- | --- | --- |
| Long agenda, phone connected | 1.0 and 1.24 | English |
| Long agenda, phone not connected (footer shown) | 1.24 | English |
| Empty, phone connected | 1.0 and 1.24 | English |
| Empty, never synced (footer shown) | 1.0 and 1.24 | each of the six at 1.24, English at 1.0 |

The never-synced state is the tallest and has the longest footer text, so it is the one run in all six languages. Empty with "Phone not connected" is left out: it is the same layout as never synced with a shorter footer.

**D7. The watch packages all six languages, and a test proves a non-Dutch translation resolves.** `resourceConfigurations` in `src/wear/build.gradle.kts` becomes `listOf("en", "nl", "de", "fr", "es", "pt")`, as on the phone, with the comment corrected. Without it the six-language cases of D6 would render English for German, French, Spanish and Portuguese and could never fail on a long translation; lint cannot catch it, because lint reads the sources and not the package. A new instrumented case asserts that the footer rendered for a Portuguese and for a German locale is not the English text.
*Alternative:* fix the packaging in a separate bug change first. Rejected as slower for a one-line fix this change cannot be verified without; the proposal names it so it is not hidden.

**D8. The existing at-rest scenarios win over the new item.** "Empty state on a round screen" and "Footer on a round screen" check every line at rest, with no scroll allowance. If the empty text, the footer and the new item together no longer fit at rest at the default font, the empty text and the footer keep their place inside the circle and the mark and version are reached by scrolling; the new scenario only promises they fit once scrolled to the end. Task 2.1 measures this and records the result here.

**D9. Check the details screen's end with the same kind of test.** It uses the same scaffold and column. If its last line already fits, the new case passes at once, and the screen's code stays as it is.

## Risks / Trade-offs

- [The empty state at the largest font, with the footer, is already taller than the screen; one more item makes it taller] → the existing "Round screen at the largest font" cases must still pass, with only their scroll-to-end changed to the real last item (D6); the new item is at the end, so it is covered by "once scrolled to the end".
- [In the empty state the mark is not at the bottom edge but under the centred text] → accepted (D1); it is still the last thing on the screen. Whether it is visible without scrolling at the default font when the footer is also shown is not known yet; D8 says what gives way if it is not.
- [Packaging four more languages surfaces a translation that is too long for the round screen] → that is the point of D7; a failing existing six-language case is fixed in this change through the translator, not by removing the language.
- [The inferred cause for the dose card is wrong and the extra item changes nothing] → task 2.2 runs the triage tests right after the item is added; if they still fail, D4's dose-row padding, then its fallback.
- [More space at the end of the list on a square watch] → check the square preview, and accept a small extra gap.
- [The scroll transformation scales the last item, so the measured outline differs from the laid-out one] → the tests map bounds through each node's drawn position, as `RoundScreenFitTest` did for #107.
- [Two copies of the placeholder mark drift apart] → both are replaced together when the final artwork lands (D2).

## Implementation record

What tasks 2.0 to 2.3 found on `Wear_Small_Round_API_36` (384 px round), and where the code departs from or fills in the decisions above.

- **D2, one more token.** Besides `WearDimens.markSize` (24 dp) there is `WearDimens.betweenMarkAndVersion` (4 dp), the gap between the mark and the version line. No existing token meant that gap, and a composable does not write a raw dp value.
- **D4, what the last item declares.** The item declares `minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding)`, the value Wear M3 gives its own `Card` at the end of a list. That was enough for the mark and the version in every state of D6's table, at font scale 1.0 and 1.24 and in all six languages. The D4 fallback (a `WearDimens` end-padding token) was not needed.
- **D4, dose rows.** Not touched. With the item under it, the last dose card scrolls up to where the circle is wider, and both end-of-list cases pass at font scale 1.0 and 1.24. `DoseCard` declares nothing. The inferred cause in Context is confirmed: without the item the two cases fail exactly as triage reported (209.29 px and 208.19 px from the centre).
- **D6, what "scrolled to the end" does in the tests.** Scrolling to the last item is not always the end: the column brings that item to the middle of the screen, and a tall last item (a schedule of several wrapped lines on the details screen) then still has list left under it. The fit tests scroll to the last item and then as far as the list still moves.
- **D6, the mark's box.** The mark is measured by its whole box as drawn (its laid-out size carried through its layers' transformations), not by the part of it that is visible, so a mark half below the screen fails rather than passing on its visible half.
- **D8, the at-rest cases.** At the default font nothing gives way: "Empty state on a round screen" and "Footer on a round screen" still hold for every line at rest, the version line included. **The never-synced empty state shows the mark and the version without scrolling at the default font**: the version line, the lowest thing on the screen, lies inside the circle at rest. At the largest font, the empty text, the footer and the new item together are taller than the screen in both footer states (never synced, and phone not connected), and the version line starts below the bottom edge. D8 applies there: the empty text and the footer keep their place, every line of them inside the circle at rest with no scroll allowance, and the mark and version are reached by scrolling, where they fit. The two cases `aWatchThatHasNeverSyncedFitsTheRoundScreenAtTheLargestFont` and `aPhoneOutOfReachFitsTheRoundScreenAtTheLargestFont` check exactly that: at rest everything but the version line, then the mark and version at the end. This is what "Round screen at the largest font" in the spec already allows for text taller than the screen.
- **D9, the details screen clips too.** Its last line crossed the bezel at the end of the list, at the default font as well as the largest ("30 tablets" at Rect(50.6, 311.6, 178.0, 343.1) at font scale 1.0). Fixed the same way as the agenda: the rows that can be last (default dose, schedule, stock, and the "no details" text) declare `minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding)`, which the scaffold applies only to the item that is last. Besides the case task 1.3 asked for, there are cases for the default font, for a schedule as the last line, and for a dose with no details.
