## MODIFIED Requirements

### Requirement: Pattern selection
The editor SHALL offer exactly three patterns: "Every N days", "Weekdays" and "Every N hours". Switching pattern MUST keep the amount and MUST keep entered times where the new pattern uses a times list. The three pattern options SHALL render at one shared height regardless of locale or system font scale, and a label MUST wrap onto a second line rather than be truncated when it does not fit on one line.

#### Scenario: Switch from every N days to weekdays
- **WHEN** the user has entered times 08:00 and 20:00 under "Every N days" and switches to "Weekdays"
- **THEN** the times list still shows 08:00 and 20:00 and the weekday chips are shown with none selected

#### Scenario: One label wraps to two lines
- **WHEN** the active locale's translation of "Every N days" or "Every N hours" is long enough to wrap onto a second line while "Weekdays" stays on one line
- **THEN** all three pattern options render at the same height, sized to fit the wrapped label, and no label is truncated or ellipsized

#### Scenario: All labels fit on one line
- **WHEN** the active locale's translations of all three pattern labels fit on one line
- **THEN** all three pattern options render at the same, single-line height
