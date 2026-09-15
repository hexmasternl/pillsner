## MODIFIED Requirements

### Requirement: Visual baseline from the design system
The welcome screen SHALL be rendered with the Pillsner theme defined in `docs/design-system.md`: the brand light and dark colour schemes following the user's stored theme choice, the bundled Montserrat and Raleway type scale, and the shape and spacing tokens. The stored theme choice defaults to following the system light/dark setting; the full behaviour of that choice is specified by `app-theme`. The app MUST NOT use Material You dynamic colour, and no screen MAY define a colour, text size or spacing that is not a theme token.

#### Scenario: Dark mode follows the system by default
- **WHEN** the user has not chosen a theme and the system is set to dark theme
- **THEN** the welcome screen renders with the dark colour scheme from the design system and the same layout

#### Scenario: Chosen theme overrides the system
- **WHEN** the user has chosen the Light theme and the system is set to dark theme
- **THEN** the welcome screen renders with the light colour scheme from the design system and the same layout

#### Scenario: Wallpaper colour ignored
- **WHEN** the device wallpaper produces a purple dynamic palette
- **THEN** the app still renders in Pillsner green, blue and white
