# Design

## Context

Both watch screens (`UpcomingDosesScreen`, `DoseDetailsScreen`) are a `ScreenScaffold` around a `TransformingLazyColumn` that takes the scaffold's `contentPadding`. In Wear Compose Material 3 1.6.2 that padding is `ScreenScaffoldDefaults.contentPadding`: 5.2% of the screen width horizontally and 10% of the height vertically. That is sized for cards, which the scroll transformation narrows as they near the top and bottom edges. Plain text gets nothing extra. The empty state and the footer add only `WearDimens.screenEdge` (8 dp), and the time headings and detail rows add `cardPaddingHorizontal` (12 dp). On the small round device (192 dp across), the empty state's first line starts about 19 dp from the top. At that height the circle is only about 115 dp wide, so the line runs into the bezel. The "No medicines" line fails even at font scale 1.0 (screenshots in triage, #107).

`wear-day-overview` is merged but not archived. Its version of "Wear form factor and accessibility" is the one this change modifies.

Locally, the only Wear image is API 37, where Espresso 3.7.0 (the newest stable) crashes in `InputManager.getInstance` for every Compose UI test. That includes the existing `UpcomingDosesScreenTest`, so the cause is the environment, not the new test.

## Goals / Non-Goals

**Goals:**
- Every line of text on the empty and footer states lies inside the circle on the small round device, at font scale 1.0 and 1.24 (the largest Wear OS font size).
- The first heading or line of both screens clears the bezel when they open scrolled to the top.
- A test that proves this on a real round Wear OS emulator, and a documented way to run the watch tests there.

**Non-Goals:**
- Redesigning the cards (`DoseCard`), which already follow the scaffold padding and the scroll transformation.
- Square-screen layout. It isn't affected and keeps its previews.
- Watch tests in CI, and the API 37 Espresso crash (see proposal, Impact).

## Decisions

**D1. Centre the empty state vertically.** When there is nothing to list, the column holds only the empty text and possibly the footer. It uses `Arrangement.spacedBy(WearDimens.betweenCards, Alignment.CenterVertically)`, so the block sits at the widest part of the circle instead of under the clock. It stays inside the `TransformingLazyColumn`, so at the largest font, if it no longer fits, it still scrolls by touch and rotary like the list.
*Alternative:* a separate non-lazy `Column` with `verticalScroll` and rotary handling. Rejected: it means a second scroll setup to keep accessible, for one state.

**D2. A round-screen inset for plain text, as a fraction of the screen width.** `WearDimens` gets a `textInsetFraction`, starting at 0.12 and tuned until `RoundScreenFitTest` passes at both font scales on the small round device. A small composable helper turns it into dp from the screen width. It replaces `screenEdge` on the empty state and the footer. A fraction scales across small, large and extra-large round devices the way the scaffold's own 5.2% does, and still follows the "no raw dp in composables" rule.
*Alternative:* a larger fixed dp. Rejected: it is too narrow on a small device or wastes width on a large one.

*Correction found during implementation:* the time headings and detail rows keep `cardPaddingHorizontal`. With the agenda and the details screen scrolled to the top, `RoundScreenFitTest` already passed on the unchanged code at both font scales: the day heading and the title are `ListHeader`s, and the scroll transformation narrows what sits under them. Widening them would have changed a layout that already meets the spec.

**D3. When the empty state overflows, it gets a heading's room at the top.** At font scale 1.24 with the footer, the text is taller than the screen, centring does nothing, and the first line starts at the scaffold's 10% top padding, where the circle is too narrow for a word as wide as "No medicines". The empty-state item gets `minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding)`: 13% of the screen height, the library's own value for a heading at the top of a list, instead of a number of ours.
*Correction found during implementation:* D3 originally applied this to the first heading of the agenda and the details title. The test showed those already fit; the empty state was what needed it.

**D4. Test the geometry, not the pixels.** `RoundScreenFitTest` (written during triage, commit `a6e189c` on the feature branch) reads each text node's `TextLayoutResult`. It turns every line's box into root coordinates and requires all four corners to lie within the circle centred on the root, with radius half the root's shorter side. It extends to font scale 1.24 through `DeviceConfigurationOverride.FontScale`, and adds a "scrolled to the top" case for both screens that checks the upper half of the screen. Lines are mapped through each node's drawn bounds, so the scroll transformation's scaling counts. The tallest empty state (never synced) runs in all six languages at 1.24: upper half at rest, lower half scrolled to the end. A line in the lower half at rest is deliberately not checked. Half a card under the bottom edge is content to scroll to, not content cut off.
*Correction found during implementation:* the language case first checked the whole screen at rest, and failed on Dutch: "synchroniseren" makes the footer taller than the screen, so at rest its last lines run into the bottom edge, as the last item of any overflowing Wear list does. The spec scenario "Round screen at the largest font" was reworded to match. It `assume`s `Configuration.isScreenRound`, so on a square device or the phone emulator it is skipped rather than failing.
*Alternative:* screenshot tests. Rejected: they need a new screenshot library (a dependency proposal of its own) and still need a person to judge "cut off".

**D5. Run watch tests on a small round Wear OS API 36 emulator.** Install the Android SDK command-line tools, add the Wear OS API 36 system image, and create an AVD named `Wear_Small_Round_API_36` (device `wearos_small_round`) with `avdmanager`. That replaces the hand-written API 37 AVD from triage, which is deleted. The `emulator-tests` skill is updated so `wear` tests run on this AVD, booted headless the same way as the phone one. The test must first be seen failing on `development`'s screen code for the reason the report gives, and only then the fix goes in.
*Alternative:* bump androidx.test to 1.8.0-alpha01 / Espresso 3.8.0-alpha01 to run on API 37. Rejected: `CLAUDE.md` forbids pre-releases.

## Risks / Trade-offs

- [The Wear OS API 36 image is not installable through cmdline-tools on this machine] → install it through Android Studio's SDK Manager instead, and record the manual step in the skill.
- [A bigger text inset makes long translated strings wrap to more lines, so the empty state no longer fits at 1.24] → D1 keeps it scrollable, and the test checks only what is on screen without scrolling. Check the German and Portuguese strings (the longest) by preview on the small round device.
- [Play may also flag a large or extra-large round device] → the inset is a fraction, so it scales. Add `WearDevices.LARGE_ROUND` previews for the empty and footer states.
- [`positionInRoot` does not reflect the scroll transformation's scaling] → only states without scrolling and the first item are checked, where the transformation is at rest (scale 1).

## Migration Plan

No data or settings change. After merge, the watch bundle goes out with the next release. Then resubmit it on the Wear track in Play Console, which closes the rejection. Roll back by reverting the PR.
