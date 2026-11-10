# Proposal

## Why

**GitHub Issue:** #116 (https://github.com/hexmasternl/pillsner/issues/116)
**Pull Request:** #117 (https://github.com/hexmasternl/pillsner/pull/117)

On a round watch, when the agenda is longer than the screen, the last dose can't be scrolled far enough up: its card stops about 40 px above the bottom edge on a small round watch, and the round edge cuts off its corners (the card reaches about 209 px from the centre of a 192 px circle). The text stays inside the circle, but the entry doesn't fit. The `wearable-app` spec already says the list scrolls "to reveal every entry" on a round screen. The app doesn't meet that, and nothing tests the end of the list: `fix-wear-round-edge-clipping` (#107) covered only the empty state, the footer and the top of the list. This is the same Wear quality guideline Play rejected the watch app for before, so it should be fixed before the next Wear release.

The product owner also asked for the agenda to end with a small Pillsner logo and the installed version, centred, and for that to sit completely on the screen. That gives the list a proper end, lets a user (and a bug report) say which watch build is installed, and puts something other than a dose card in the narrowest part of the circle.

## What Changes

- The agenda screen ends with a new last item: the product mark, small, with the installed version name under it ("Version 0.1.0"), horizontally centred. It is shown below the doses or the empty state, and below the phone connection footer when that is shown. It is not tappable.
- Scrolled to the end, the whole mark and the whole version line sit inside the circle of a round screen, at the default and at the largest watch font size, in every supported language.
- At the end of a long agenda, the last dose card can be scrolled far enough that its whole outline sits inside the circle, at the default and at the largest watch font size.
- The details screen's last line gets the same check at the end of its list. The details screen does not get the mark or the version.
- `RoundScreenFitTest` gets cases that measure the outline of the last card (its shape, not just its text) against the circle once the list is scrolled to the end. Triage already wrote and committed them on `feature/fix-watch-agenda-bottom-inset` (`f56aa53`). New cases measure the mark and the version line the same way.
- The watch app packages all six languages. Today `src/wear/build.gradle.kts` keeps only English and Dutch (`resourceConfigurations += listOf("en", "nl")`), so a watch following a German, French, Spanish or Portuguese phone shows English, against "Language follows the phone app". The phone had the same defect and fixed it in `app-language-german-string-resolution-fix`; the watch line was missed. It is fixed here because this change's six-language fit checks would otherwise measure English four times and prove nothing.
- Nothing changes in which doses the watch shows or in what order.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `wearable-app`:
  - A new requirement, "App mark and version on the agenda": what the last item shows, where, and that it comes from the build configuration.
  - "Wear form factor and accessibility" gets two concrete scenarios for the end of the list on a round screen: the last dose entry, and the mark with the version, each lie entirely inside the circle once scrolled to the end, at the default and the largest font size. The existing "Round screen" scenario already implies the first; the new scenarios make both testable, the way #107 did for the top.
  - "Language follows the phone app": the version line is translated and follows the phone app's language like the rest of the watch text.

## Impact

- **Code**: `src/wear/.../ui/UpcomingDosesScreen.kt` (the new last item), `WearDimens.kt` (a token for the mark's size), `MainActivity.kt` (passes the version name in), `src/wear/build.gradle.kts` (turns on `BuildConfig` so the version name comes from the build, and packages all six languages), a copy of the phone's `ic_pillsner_logo.xml` in the watch's drawables, and `DoseCard.kt` / `DoseDetailsScreen.kt` only if their end still clips. Nothing in the phone app, `src/shared` or the sync payload.
- **Tests**: `src/wear/src/androidTest/.../ui/RoundScreenFitTest.kt` (the triage reproduction, already committed, plus the mark and version cases) and `UpcomingDosesScreenTest.kt` (the item is present, last, and read by a screen reader as the version). Runs on `Wear_Small_Round_API_36`, and in CI's instrumented job.
- **Strings**: one new watch string, "Version %1$s", in six locales.
- **Dependencies, schema**: none.
- **Website**: checked through `website-feature-sync`; the watch page changes only if it describes or pictures the end of the agenda.
- **Release**: the watch bundle goes to Play's Wear track with the next release.
