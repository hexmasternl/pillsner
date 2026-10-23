## ADDED Requirements

### Requirement: Medicine tile stock heads-up
A medicine tile SHALL show a distinct stock heads-up indicator, recomputed live whenever the tile renders, for any medicine that has at least one stock batch recorded. The indicator SHALL state low stock when the medicine's current remaining stock does not cover its projected usage for the next 7 days, and/or SHALL state an expiry state when the soonest-expiring batch that still has remaining stock is within 30 days of, or past, its expiry date; both MAY be shown together when both apply. A medicine with no stock batches recorded SHALL show no stock indicator at all. The distinction between low stock, approaching expiry and past expiry MUST NOT rely on colour alone, and a screen reader focusing the tile MUST announce whichever states apply as part of the tile's single combined announcement.

#### Scenario: No stock tracked
- **WHEN** a medicine has no stock batches recorded
- **THEN** its tile shows no stock indicator, regardless of its schedule

#### Scenario: Stock is sufficient
- **WHEN** a medicine's remaining stock covers at least 7 days of its projected usage and its nearest batch is not near expiry
- **THEN** its tile shows no stock indicator

#### Scenario: Low stock
- **WHEN** a medicine's remaining stock would not cover the next 7 days of its projected usage
- **THEN** its tile shows an indicator stating stock is low

#### Scenario: Nearest batch approaching expiry
- **WHEN** a medicine's soonest-expiring batch with remaining stock expires in 10 days
- **THEN** its tile shows an indicator stating that stock expires soon

#### Scenario: Nearest batch past expiry
- **WHEN** a medicine's soonest-expiring batch with remaining stock has an expiry date in the past
- **THEN** its tile shows an indicator stating that stock has expired

#### Scenario: Low stock and expiring together
- **WHEN** a medicine is both low on stock and its nearest batch is approaching expiry
- **THEN** its tile shows both states, distinguishable from each other without relying on colour alone

#### Scenario: Live, not tied to the last dose taken
- **WHEN** a medicine's remaining stock falls below the 7-day threshold for reasons other than a dose being taken (for example, time passing so the projected week's usage grows relative to unchanged stock)
- **THEN** the tile shows the low-stock indicator the next time the Medicines screen renders, without requiring a dose to have just been taken

#### Scenario: Indicator applies to inactive tiles too
- **WHEN** an inactive medicine has low stock
- **THEN** its tile shows both the inactive distinction and the stock indicator

#### Scenario: Screen reader announces stock state
- **WHEN** a screen reader focuses a tile whose medicine is low on stock
- **THEN** the announcement includes the medicine name, its schedule description(s), and that stock is low, as a single combined item
