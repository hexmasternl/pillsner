## Why

**GitHub Issue:** #82 (https://github.com/hexmasternl/pillsner/issues/82)

The Medicine details screen shows every field of a medicine at the same weight, so the name and default dose, the two things a user actually comes to check, sit in a dense column together with used since, use until, prescribed by and the active switch, and the schedules and stock sections only appear after scrolling past all of it. Issue #82 asks for the rarely-needed fields to step back so the screen reads at a glance.

## What Changes

- The Medicine details screen (the medicine form in edit mode) is reorganised into four blocks, top to bottom:
  1. **Primary details**, always visible: name and default dose.
  2. **Secondary details**, collapsed by default: used since, use until, prescribed by and the Active switch, behind a "More details" toggle button. Tapping it expands the panel and the button reads "Less details"; tapping again collapses it.
  3. **Schedules**, always visible, unchanged.
  4. **Stock**, always visible, unchanged.
- The panel opens on its own when Save is tapped and a field inside it has a validation error, so an error is never hidden behind the toggle.
- Expanding or collapsing the panel is not an edit: it never triggers the discard confirmation and never changes what is saved. The open/closed state survives rotation and process death like the rest of the screen, and each fresh open of the details screen starts collapsed.
- The **Add medicine** form is unchanged: a new medicine still shows every field, since the user is filling them in for the first time.
- Two new label strings ("More details", "Less details") and two state-description strings, each with translations in every supported language, and one new icon drawable (`expand_more`).

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `medicine-details`: the "Medicine details screen opens pre-populated" requirement gains the four-block order and the collapsed-by-default secondary details; a new "Secondary details toggle" requirement defines the toggle, its default, its behaviour on validation errors, its independence from the edited draft and its persistence; the "Details screen accessibility" requirement covers the toggle's spoken label, state and touch target.

## Impact

- `src/app/src/main/java/nl/hexmaster/pillsner/ui/medicines/form/MedicationFormScreen.kt`: the field column is regrouped, a toggle button and an animated panel are added, previews are updated.
- `src/app/src/main/java/nl/hexmaster/pillsner/ui/medicines/form/MedicationFormUiState.kt` and `MedicationFormViewModel.kt`: one boolean of UI state, one event, one saved-state key, and a small addition to the save path that expands the panel on a hidden error.
- `src/app/src/main/res/values*/strings.xml`: four strings in six languages; `res/drawable/ic_expand_more.xml`.
- Tests: `MedicationFormViewModelTest` (unit), `MedicationFormFlowTest` and `MedicationDetailsFlowTest` (instrumented). Edit-mode assertions on the secondary fields must expand the panel first, plus new cases for the toggle itself.
- No domain, data, scheduling, reminder or persistence-schema change. No new dependency, permission or network access.
- The still-active `medicine-expiry-tracking` and `medicine-details-menu-shortcuts` changes also touch this screen; this change stays out of the overflow menu and the Stock section so the code and spec deltas do not overlap.
