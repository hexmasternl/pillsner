# app-theme Specification

## Purpose
TBD - created by archiving change app-theme-setting. Update Purpose after archive.
## Requirements
### Requirement: Theme options
The app SHALL offer exactly three themes: System default, Light and Dark. System default means the app follows the phone's own light/dark setting; Light means the light colour scheme regardless of the phone; Dark means the dark colour scheme regardless of the phone. System default SHALL be what applies until the user chooses otherwise.

The rule that turns a theme choice and the phone's current light/dark state into a rendered scheme MUST be implemented without Android framework dependencies and MUST be unit-tested.

#### Scenario: Three options and no more
- **WHEN** the theme options are read
- **THEN** they are exactly System default, Light and Dark, in that order

#### Scenario: Default before any choice
- **WHEN** the app is installed and the user has never opened the Theme setting
- **THEN** the effective theme is System default and the app renders the scheme matching the phone

#### Scenario: Resolution truth table
- **WHEN** the resolution rule is given each of the three choices against a phone that is in light mode and a phone that is in dark mode
- **THEN** System default resolves to light and dark respectively, Light resolves to light in both cases, and Dark resolves to dark in both cases

### Requirement: Theme section on Settings
The Settings screen SHALL show a Theme section, placed after the Language section and before the Security section, containing a dropdown labelled "Theme" whose current value is the stored selection. The options SHALL be, in order, "System default", "Light" and "Dark". The section header and every option SHALL come from string resources and be translated.

#### Scenario: Default display
- **WHEN** no theme has ever been chosen and the user opens Settings
- **THEN** the dropdown shows "System default"

#### Scenario: Options listed
- **WHEN** the user opens the dropdown
- **THEN** the options read "System default", "Light" and "Dark" in that order

#### Scenario: Stored selection shown
- **WHEN** the user previously chose Dark and reopens Settings
- **THEN** the dropdown shows "Dark"

#### Scenario: Position on the screen
- **WHEN** the Settings screen is rendered
- **THEN** the Theme section appears below the Language section and above the Security section

#### Scenario: Dutch copy
- **WHEN** the app runs in Dutch
- **THEN** the section reads "Thema" and the options read "Systeemstandaard", "Licht" and "Donker"

### Requirement: Choosing a theme applies immediately
Selecting a theme SHALL persist the choice on the device at once, without a confirmation step, and SHALL repaint the running app in the chosen scheme without a restart. The Theme section MUST NOT show a restart notice, because none is needed.

#### Scenario: Switch to dark while light is showing
- **WHEN** the app is rendering the light scheme and the user selects "Dark"
- **THEN** the Settings screen the user is looking at repaints in the dark scheme without any further action

#### Scenario: Switch back
- **WHEN** the app is rendering the dark scheme and the user selects "Light"
- **THEN** the screen repaints in the light scheme

#### Scenario: No restart notice
- **WHEN** the user selects any theme
- **THEN** no notice about restarting the app is shown

#### Scenario: Selection survives leaving Settings
- **WHEN** the user selects "Dark", navigates to Home and back to Settings
- **THEN** the dropdown still shows "Dark" and the app is still in the dark scheme

### Requirement: The chosen theme survives a restart and is applied before the first frame
The theme choice SHALL be persisted on the device and SHALL be read before the first screen is composed, so that the app never renders a frame in a scheme other than the chosen one.

#### Scenario: Choice survives a cold start
- **WHEN** the user selects "Dark", the app is stopped and started again
- **THEN** the app starts in the dark scheme and the dropdown shows "Dark"

#### Scenario: No flash of the other scheme
- **WHEN** "Light" is stored and the phone is in dark mode, and the app is cold started
- **THEN** the first frame drawn is the light scheme, with no intermediate dark frame

### Requirement: Phone theme change while following the system
While System default is selected, a change to the phone's light/dark setting SHALL be reflected by the app without any action from the user. While Light or Dark is selected, a change to the phone's setting SHALL have no effect on the app.

#### Scenario: Following the phone
- **WHEN** System default is selected, the app is in the light scheme, and the user switches the phone to dark mode
- **THEN** the app renders the dark scheme

#### Scenario: Fixed light theme ignores the phone
- **WHEN** Light is selected and the user switches the phone to dark mode
- **THEN** the app stays in the light scheme

#### Scenario: Fixed dark theme ignores the phone
- **WHEN** Dark is selected and the user switches the phone to light mode
- **THEN** the app stays in the dark scheme

### Requirement: The whole app follows the chosen theme
Every screen, dialog, sheet and menu in the phone app SHALL render in the chosen theme, including the app lock screens shown before the app content. The status bar and navigation bar icon appearance SHALL follow the chosen theme rather than the phone's setting. The two colour schemes themselves SHALL be unchanged by this capability, and Material You dynamic colour SHALL remain switched off.

#### Scenario: Locked app follows the choice
- **WHEN** Dark is selected and the app starts locked
- **THEN** the unlock screen renders in the dark scheme

#### Scenario: System bars follow the app
- **WHEN** Light is selected and the phone is in dark mode
- **THEN** the app renders the light scheme with dark status and navigation bar icons

#### Scenario: Palettes unchanged
- **WHEN** the light and dark schemes are compared with the design system
- **THEN** every colour role matches `docs/design-system.md` section 2, unchanged by this capability

#### Scenario: Wallpaper colour still ignored
- **WHEN** the device wallpaper produces a purple dynamic palette under any of the three theme choices
- **THEN** the app still renders in Pillsner green, blue and white

### Requirement: Theme setting is stored on the device only
The theme setting SHALL be stored in the app's general settings store on the device, alongside the language, and SHALL be kept separate from the app lock settings. An absent, empty or unrecognised stored value SHALL be treated as System default.

#### Scenario: Stored beside the language
- **WHEN** the theme is written
- **THEN** it is stored in the general settings store and not in the app lock settings store

#### Scenario: Corrupt stored value
- **WHEN** the stored value is not one of the recognised theme values
- **THEN** the app treats it as System default and follows the phone

#### Scenario: Nothing leaves the device
- **WHEN** the theme is chosen or read
- **THEN** no network request is made and no data is sent off the device

### Requirement: The watch is not affected
The phone's theme choice SHALL NOT be sent to a paired wearable and SHALL NOT change how the wearable app renders. The wearable keeps its own dark palette.

#### Scenario: Light on the phone, dark on the watch
- **WHEN** Light is selected on the phone and a paired watch shows a dose
- **THEN** the watch renders in its own dark palette and the synced payload carries no theme field

