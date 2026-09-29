## Context

The Medicine details screen is `MedicationFormScreen` in edit mode (`MedicationFormMode.Edit`), driven by `MedicationFormViewModel` and `MedicationFormUiState`. Its content is one vertically scrolling `Column` that lays out, in order: name, default dose, used since, use until, prescribed by, the Active switch (edit mode only), the Schedules header and rows with "Add schedule", and the Stock section (edit mode only, from the still-active `medicine-expiry-tracking` change). The edited draft lives in a `SavedStateHandle`-backed `MedicationFormDraft`, and `hasEdits` is derived by comparing the draft with the values that were loaded.

The current order already matches the four blocks issue #82 asks for; what changes is that the middle block becomes collapsible and starts collapsed. The Add medicine form shares this composable and must keep showing every field.

Constraints from `docs/design-system.md` and `CLAUDE.md`: MVVM with unidirectional data flow, tokens only (no inline sizes or colours), 48 dp minimum touch targets, medium motion duration of 250 ms with reduce-motion honoured, strings in resources with all six translations, and `pillsner-ui-review` before the UI task is declared done.

## Goals / Non-Goals

**Goals:**
- Name and default dose are the first things on the details screen, with nothing else between them and the toggle.
- Used since, use until, prescribed by and Active are hidden behind a single "More details" / "Less details" toggle, collapsed on every fresh open.
- A validation error in a hidden field can never leave the user staring at a Save button that does nothing.
- Toggling is pure presentation: no effect on `hasEdits`, on what is saved, or on any other screen.
- The panel's open/closed state survives rotation and process death together with the draft.
- Screen readers announce the toggle's label and its expanded/collapsed state.

**Non-Goals:**
- Changing the Add medicine form. It keeps every field visible.
- Changing which fields exist, their validation, the schedules section, the stock section or the overflow menu.
- Remembering the panel state across separate opens of the details screen (it always starts collapsed).
- Any change to domain, data or scheduling code.

## Decisions

**1. The expansion flag lives in the view model's UI state, not in `remember` inside the composable.**
`MedicationFormUiState` gains `secondaryDetailsExpanded: Boolean = false`, and the view model gains `onSecondaryDetailsToggled()`. The alternative, `rememberSaveable` in the screen, is less code, but the save path needs to force the panel open when a hidden field is invalid, and that decision belongs where validation happens. Keeping it in the view model also keeps the screen a pure function of state, as the rest of the form is.

**2. The flag is stored in the `SavedStateHandle` alongside the draft, but it is not part of `MedicationFormDraft`.**
Storing it under its own key (for example `secondary_details_expanded`) means process death restores the panel as the user left it, in keeping with the existing "Edited draft survives configuration changes" requirement. Keeping it out of the draft means `hasEdits`, which is `draft != initialDraft`, is untouched by toggling, so the discard dialog never fires because of it.

**3. `save()` expands the panel when a hidden field is invalid.**
Today the only validation that belongs to a secondary field is `useUntilError` (use until before used since). When `save()` sets `showErrors = true` and `canSave` is false, it also sets `secondaryDetailsExpanded = true` if any secondary-field error is non-null. The rule is written generically ("a field inside the panel has an error") so a future secondary-field validation inherits it. The alternative of showing the error text next to the toggle was rejected: it duplicates error presentation and still leaves the field itself out of reach.

**4. The toggle is a full-width `TextButton` with a trailing `expand_more` chevron that rotates 180 degrees when expanded.**
Per design system section 8.4, tonal buttons are second-rank actions and text buttons are low-emphasis; the toggle is navigation within the form, not an action on the medicine, so text emphasis is right. It sits directly under the default dose field and above the panel, height at least `Sizes.minTouchTarget`, label in `labelLarge` from `R.string.medicine_details_more` / `R.string.medicine_details_less`. The chevron uses `contentDescription = null` because the label already says what happens. The button carries `semantics { stateDescription = ... }` with "expanded" / "collapsed" from string resources so TalkBack announces "More details, button, collapsed". The icon is a new `ic_expand_more.xml` vector in the existing Material Symbols Rounded style, matching `ic_chevron_right.xml`.

**5. The panel is `AnimatedVisibility` with `expandVertically` / `shrinkVertically` at the medium duration token.**
Compose honours the system animator duration scale, so "Remove animations" collapses the duration to zero as section 9 requires. The panel keeps the same `Spacing.lg` vertical rhythm as the rest of the column so the expanded layout is identical to today's layout below the dose field.

**6. Add mode renders the fields exactly as today.**
The screen reads `uiState.showsSecondaryDetailsToggle` (true only in edit mode, like `showsActiveSwitch`). In add mode the toggle is absent and the four fields are laid out inline, so `MedicationFormFlowTest`'s add-mode assertions and the add form's previews are unaffected.

**7. Instrumented tests expand the panel through the toggle, not by poking view model state.**
Existing edit-mode assertions that scroll to used since, use until, prescribed by or the Active switch first click the toggle by test tag (`MedicationFormTestTags.SECONDARY_DETAILS_TOGGLE`). This keeps the tests describing what a user does, and a test that the fields are absent before the tap becomes the regression guard for the default.

## Risks / Trade-offs

- [Users who edit "use until" often now need one extra tap] → The toggle is directly under the dose field, one tap, and the panel state survives rotation, so the cost is a single tap per visit. Issue #82 explicitly accepts this in exchange for a readable screen.
- [An error in a hidden field could be invisible] → Decision 3 opens the panel whenever Save fails on a secondary field, and the delta spec has a scenario for it.
- [Two other active changes touch this screen] → `medicine-expiry-tracking` owns the Stock section and `medicine-details-menu-shortcuts` owns the overflow menu. This change only regroups the field column, and its delta spec does not modify "Details screen title and actions", which those two changes modify. Whichever archives first, the others rebase cleanly.
- [TalkBack traversal after toggling] → Focus stays on the toggle; the newly visible fields are its next siblings in the column. Automated semantics coverage exists, but the manual TalkBack traversal check remains open in task 5.3.
- [Largest-font-scale layout] → The expanded state is the same layout as today, which the existing "Largest font scale" scenario already covers; the collapsed state is strictly shorter.

## Corrections found during implementation

- **Motion tokens did not exist.** Decision 5 assumed a "medium duration token"; the theme had none. `ui/theme/Motion.kt` now holds the three durations from design system section 9 (150, 250, 350 ms), and the panel and chevron use `Motion.MEDIUM_MILLIS`. Compose's Android host scales every animation by the system animator duration scale, so "Remove animations" needs no further code.
- **Decision 7 named the wrong test file.** `MedicationFormFlowTest` only ever opens the form in add mode, so it needed no change; the edit-mode cases went into `MedicationDetailsFlowTest`, and a new `SecondaryDetailsToggleTest` covers the toggle's semantics, size and add-mode absence in isolation.
- **Two spec scenarios are unit-tested rather than driven through the UI.** "Hidden error opens the panel" would need the Material date picker driven in an instrumented test, which nothing in this project does yet; "State survives process death" and rotation are covered by rebuilding the view model on the same `SavedStateHandle`. Both live in `MedicationFormEditModeTest`.

## Open Questions

_None._ The one judgement call, leaving the Add medicine form unchanged, is stated in the proposal; if the product owner wants the collapse there too it is a one-line change to `showsSecondaryDetailsToggle` and a spec tweak.
