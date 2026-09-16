## MODIFIED Requirements

### Requirement: Pattern selection
The editor SHALL offer exactly three patterns: "Every N days", "Weekdays" and "Every N hours". Switching pattern MUST keep the amount and MUST keep entered times where the new pattern uses a times list. The three pattern options SHALL render at one shared height regardless of locale or system font scale, and a label MUST wrap onto as many lines as it needs rather than be truncated or clipped when it does not fit on one line.

#### Scenario: Switch from every N days to weekdays
- **WHEN** the user has entered times 08:00 and 20:00 under "Every N days" and switches to "Weekdays"
- **THEN** the times list still shows 08:00 and 20:00 and the weekday chips are shown with none selected

#### Scenario: One label wraps to more lines than another
- **WHEN** the active locale's translation of "Every N days" or "Every N hours" is long enough to wrap onto more lines than "Weekdays" needs
- **THEN** all three pattern options render at the same height, sized to fit the label that needs the most lines, and no label is truncated or ellipsized

#### Scenario: A label needs more lines at a larger font scale
- **WHEN** the system font scale is large enough that a label which fits on two lines at the default scale now needs three or more
- **THEN** all three pattern options grow to that label's height and no word is clipped or hidden

#### Scenario: All labels fit on one line
- **WHEN** the active locale's translations of all three pattern labels fit on one line
- **THEN** all three pattern options render at the same, single-line height
