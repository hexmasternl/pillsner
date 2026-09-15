# app-navigation Specification

## Purpose
TBD - created by archiving change app-welcome-screen. Update Purpose after archive.
## Requirements
### Requirement: Single-activity shell
The app SHALL consist of a single activity that hosts a Compose navigation host. All screens MUST be composable destinations within that host.

#### Scenario: App launch
- **WHEN** the user opens the app from the launcher
- **THEN** one activity starts and renders the navigation host with the welcome screen as its content

### Requirement: Bottom navigation bar with three destinations
The app SHALL show a bottom navigation bar with exactly three items, in this order: Home, Medicines, Settings. Each item MUST have an icon and a text label sourced from string resources; labels MUST always be visible and MUST NOT be truncated. The selected item MUST be indicated by a filled icon and an indicator, the others by outlined icons, as the design system defines. The bar MUST be visible on each of these three top-level destinations. At medium window width and above the same three items SHALL be presented as a navigation rail instead of a bottom bar.

#### Scenario: Bar visible on Home
- **WHEN** the welcome screen (Home) is displayed
- **THEN** the bottom bar shows Home, Medicines and Settings items with labels

#### Scenario: Bar visible on Medicines and Settings
- **WHEN** the user is on the Medicines or the Settings destination
- **THEN** the bottom bar remains visible with all three items

#### Scenario: Rail on a wide screen
- **WHEN** the app runs in a window of medium width or wider, such as a tablet or an unfolded foldable
- **THEN** the three items are shown in a navigation rail and content is constrained to the design system's column width

### Requirement: Home is the start destination
The Home item SHALL be selected and the welcome screen SHALL be shown when the app launches.

#### Scenario: Selected item on launch
- **WHEN** the app launches
- **THEN** the Home item is indicated as selected and neither Medicines nor Settings is selected

### Requirement: Navigating between top-level destinations
Tapping a bottom navigation item SHALL navigate to that destination and mark it as selected. Tapping Home SHALL return to the welcome screen. Tapping the already selected item MUST NOT create a duplicate destination on the back stack.

#### Scenario: Navigate to Medicines
- **WHEN** the user taps Medicines from Home
- **THEN** the Medicines destination is shown and the Medicines item is indicated as selected

#### Scenario: Navigate to Settings
- **WHEN** the user taps Settings from any top-level destination
- **THEN** the Settings destination is shown and the Settings item is indicated as selected

#### Scenario: Return to Home
- **WHEN** the user is on Settings and taps Home
- **THEN** the welcome screen is shown and the Home item is indicated as selected

#### Scenario: Re-tapping the selected item
- **WHEN** the user taps Medicines while already on Medicines
- **THEN** the destination is unchanged and pressing back afterwards behaves exactly as it would have without the extra tap

### Requirement: Back behaviour between top-level destinations
Pressing back on Medicines or Settings SHALL return to the welcome screen. Pressing back on the welcome screen SHALL leave the app.

#### Scenario: Back from Medicines
- **WHEN** the user navigated Home → Medicines and presses back
- **THEN** the welcome screen is shown and the Home item is selected

#### Scenario: Back from Home
- **WHEN** the user is on the welcome screen with no other destination on the back stack and presses back
- **THEN** the activity finishes

### Requirement: Placeholder Medicines and Settings destinations
The Medicines destination SHALL display the medicine overview screen. The Settings destination SHALL display the settings screen: a scrolling list of sections under the title "Settings", with the Language section first. Other changes add their sections beneath it.

#### Scenario: Medicines shows the overview
- **WHEN** the Medicines destination is shown
- **THEN** the medicine overview screen is displayed with its add button

#### Scenario: Settings shows sections
- **WHEN** the Settings destination is shown
- **THEN** a screen titled "Settings" is displayed whose first section is "Language" with the language dropdown

#### Scenario: Sections scroll at large font
- **WHEN** the system font scale is at maximum and the settings screen has more than one section
- **THEN** every section is reachable by scrolling and no text is clipped

### Requirement: Navigation items are accessible
Each bottom navigation item SHALL be operable with a screen reader and MUST expose its label and selected state.

#### Scenario: Screen reader on a navigation item
- **WHEN** a screen reader focuses the Medicines item while Home is selected
- **THEN** it announces the label "Medicines" and that the item is not selected

### Requirement: Add medicine destination
The navigation host SHALL include a nested medicine form flow whose start destination is the medicine form, reachable from the Medicines screen in two ways: the add button opens it in add mode with no medicine identifier, and tapping a medicine tile opens it in edit mode carrying that medicine's identifier as a route argument. The bottom navigation bar MUST be hidden throughout the flow. Back from the form SHALL return to Medicines, subject to the form's discard confirmation when it has unsaved edits. Saving SHALL return to Medicines and remove the whole flow from the back stack. The identifier MUST survive configuration changes and process death as part of the route.

#### Scenario: Reached from the add button
- **WHEN** the user taps the add button on the Medicines screen
- **THEN** the form is shown in add mode titled "Add medicine" and the bottom navigation bar is not visible

#### Scenario: Reached from a tile
- **WHEN** the user taps a medicine tile on the Medicines screen
- **THEN** the form is shown in edit mode titled "Medicine details" for that medicine and the bottom navigation bar is not visible

#### Scenario: Back returns to Medicines
- **WHEN** the user is on an untouched form, in either mode, and presses back or the back affordance
- **THEN** the Medicines destination is shown with the Medicines item selected and the bottom navigation bar visible again

#### Scenario: Save returns to Medicines
- **WHEN** the user saves a valid medicine, in either mode
- **THEN** the Medicines destination is shown, and pressing back afterwards goes to Home rather than back into the flow

#### Scenario: Identifier survives process death
- **WHEN** the process is killed and restored while the form is open in edit mode
- **THEN** the form is restored in edit mode for the same medicine

### Requirement: Schedule editor destination
The medicine form flow SHALL include a nested Schedule editor destination reached from the form's "Add schedule" button or from tapping an existing schedule row, in both add and edit mode. It SHALL share the form's draft so that the schedule list persists across the two screens. The bottom navigation bar MUST remain hidden. Back and Done SHALL both return to the form.

#### Scenario: Open the editor
- **WHEN** the user taps "Add schedule" on the form
- **THEN** the Schedule editor is shown and the bottom navigation bar is not visible

#### Scenario: Open the editor for a saved schedule
- **WHEN** the form is in edit mode and the user taps a schedule row that was loaded from the saved medicine
- **THEN** the Schedule editor is shown with that schedule's amount, pattern and times

#### Scenario: Done returns to the form with the draft intact
- **WHEN** the user completes a schedule and taps Done
- **THEN** the form is shown with the fields entered earlier still present and the new schedule listed

#### Scenario: Back returns to the form
- **WHEN** the user presses back in the editor
- **THEN** the form is shown with its draft unchanged

#### Scenario: Rotation inside the editor
- **WHEN** the device rotates while the editor is open
- **THEN** the editor is still shown with its entered values and the form's draft is preserved behind it

### Requirement: Usage history destination
The medicine form flow SHALL include a nested Usage history destination, reached only from the overflow menu of the medicine form in edit mode, carrying the medicine's identifier as a route argument. The bottom navigation bar MUST remain hidden on it. The destination SHALL take its own view model rather than the form's shared draft, so nothing it does can touch the draft. Back SHALL return to the form with its draft, including unsaved edits, exactly as it was. Leaving the whole flow SHALL remove the Usage history destination from the back stack with it. The identifier MUST survive configuration changes and process death as part of the route.

#### Scenario: Open from the details screen
- **WHEN** the user taps the overflow action on the medicine form in edit mode and chooses "Usage history"
- **THEN** the Usage history destination is shown for that medicine and the bottom navigation bar is not visible

#### Scenario: Back returns to the form with the draft intact
- **WHEN** the user has changed the dose on the form, opens the Usage history and presses back
- **THEN** the form is shown with the changed dose still present, and no discard confirmation was asked

#### Scenario: Not reachable while adding
- **WHEN** the form is open in add mode
- **THEN** there is no way to reach the Usage history destination

#### Scenario: Saving clears the whole flow
- **WHEN** the user returns from the Usage history to the form and saves
- **THEN** the Medicines destination is shown, and pressing back afterwards goes to Home rather than back into the flow or the Usage history

#### Scenario: Identifier survives process death
- **WHEN** the process is killed and restored while the Usage history destination is open
- **THEN** the Usage history is restored for the same medicine

#### Scenario: Rotation on the Usage history
- **WHEN** the device rotates while the Usage history is open
- **THEN** the same medicine's history is shown with the same period selected

### Requirement: About destination
The navigation host SHALL include an About destination that is not top-level, reached only from the About row on the Settings screen. Its route MUST carry no arguments. The bottom navigation bar or rail MUST be hidden while it is shown. Back from the About screen, by either the back affordance or the system back gesture, SHALL return to Settings, and the Settings item SHALL then be selected again.

#### Scenario: Reached from Settings
- **WHEN** the user taps the About row on the Settings screen
- **THEN** the About destination is shown and the bottom navigation bar is not visible

#### Scenario: Back returns to Settings
- **WHEN** the user presses back or the back affordance on the About destination
- **THEN** the Settings destination is shown with the Settings item selected and the bottom navigation bar visible again

#### Scenario: Not a navigation item
- **WHEN** the bottom navigation bar is shown on any top-level destination
- **THEN** it still shows exactly the three items Home, Medicines and Settings, and no About item

#### Scenario: Back from About continues to Home
- **WHEN** the user navigated Home → Settings → About and presses back twice
- **THEN** the welcome screen is shown and the Home item is selected

