## MODIFIED Requirements

### Requirement: Welcome screen header
The welcome screen SHALL display the Pillsner logo and the app title "Pillsner" at the top of the screen. The title MUST come from a string resource and the logo MUST be a drawable resource that can be replaced without code changes.

The title SHALL be rendered as a two-colour wordmark that shows the two words the name blends: the leading fragment "Pills" in the secondary colour role and the trailing fragment "ner" in the primary colour role, in both the light and the dark scheme. The two fragments MUST render as one unbroken word with no inserted space or gap, in a single text element, at one type role and one font weight. The colours MUST come from the theme's colour scheme and MUST NOT be hard-coded in the screen.

The colouring is decorative. The title MUST remain a single element whose text is the whole word "Pillsner", so that assistive technologies announce it as one word and text matching finds it as one word.

#### Scenario: Header visible on launch
- **WHEN** the app is launched and the welcome screen is shown
- **THEN** the logo image and the title "Pillsner" are visible above any dose content

#### Scenario: Title is split into two colours
- **WHEN** the welcome screen header is rendered
- **THEN** the characters "Pills" use the secondary colour role and the characters "ner" use the primary colour role, with no gap between them and no difference in size or weight

#### Scenario: Both schemes
- **WHEN** the device is in dark mode
- **THEN** the two fragments use the dark scheme's secondary and primary colours, and both remain legible against the surface

#### Scenario: Logo has an accessible description
- **WHEN** a screen reader focuses the header
- **THEN** the logo exposes a content description naming the app, and the title is read as text

#### Scenario: Title is one node for accessibility and tests
- **WHEN** a screen reader focuses the title, or a test matches on the text "Pillsner"
- **THEN** exactly one element is found and its text is "Pillsner", not two separate elements reading "Pills" and "ner"

#### Scenario: Title resource does not contain the accent fragment
- **WHEN** the app title resource is changed to a name that does not end in "ner"
- **THEN** the whole title is rendered in the leading colour as one word, and the header renders without error
