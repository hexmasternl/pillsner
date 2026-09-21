## ADDED Requirements

### Requirement: Single per-minute update source

The watch screen SHALL derive all of its per-minute-driven updates (the list contents and the phone-connectivity state) from exactly one per-minute timer per screen session. The app MUST NOT run a second, independent per-minute timer to compute connectivity or any other per-tick value.

#### Scenario: One tick, one connectivity check

- **WHEN** the per-minute timer fires once
- **THEN** the phone-connectivity check runs exactly once for that tick, not twice

#### Scenario: One tick, one list recomputation

- **WHEN** the per-minute timer fires once
- **THEN** the upcoming-doses list is recomputed exactly once for that tick, not twice
