## MODIFIED Requirements

### Requirement: Daily refresh
The app SHALL wake once per day shortly after local midnight to extend the two-day window, even
when no dose or snooze is pending sooner. This daily wake SHALL also be armed whenever the app has
any stored dose history at all, even when no medication is currently active and scheduled, so that
housekeeping work that rides this wake (such as `dose-history-retention`'s purge) still gets a
chance to run for a user with no currently active schedule.

#### Scenario: Quiet day
- **WHEN** the only medicine is due every other day and today has no dose
- **THEN** an alarm is set for shortly after midnight and after it fires tomorrow's dose exists and
  the alarm is set for it

#### Scenario: No active medication but dose history exists
- **WHEN** every medication is inactive or has no schedule, and at least one dose row is stored
- **THEN** an alarm is still set for shortly after midnight, and the wake it triggers runs normally

#### Scenario: Nothing active and nothing stored
- **WHEN** no medication is active with a schedule and no dose has ever been stored
- **THEN** no daily-refresh alarm is armed on that account alone
