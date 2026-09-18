## ADDED Requirements

### Requirement: What counts as expired dose history
A dose row SHALL be eligible for deletion once its scheduled moment falls before the retention cutoff (one year back from the trusted-now date, by local calendar date — see "Retention cutoff date arithmetic"). Only dose rows are ever deleted; `Medication` and `Schedule` rows are never affected by this feature, consistent with `medication-persistence`'s "Medications are never removed" requirement.

#### Scenario: A dose older than the cutoff is deleted
- **WHEN** a dose's scheduled moment falls before the retention cutoff
- **THEN** that dose row no longer exists after the next wake

#### Scenario: A dose on or after the cutoff is kept
- **WHEN** a dose's scheduled moment falls on or after the retention cutoff
- **THEN** that dose row is unaffected

#### Scenario: A pending dose is never old enough to be affected
- **WHEN** the purge runs
- **THEN** no pending dose (one with no recorded outcome) is deleted, because the rolling two-day planning window never lets one reach the retention cutoff

#### Scenario: Medications and schedules are untouched
- **WHEN** the purge deletes one or more dose rows for a medicine
- **THEN** that medicine's row and its schedules are unaffected

### Requirement: Retention cutoff date arithmetic
The retention cutoff SHALL be computed as the local calendar date one year before the trusted-now date, in the device time zone, converted to the start of that local day. This mirrors the local calendar-date arithmetic `medicine-usage-history`'s "The window a period covers" requirement already uses, so leap days and daylight-saving transitions need no bespoke handling beyond ordinary calendar-date subtraction.

#### Scenario: An ordinary year
- **WHEN** the trusted-now date is 18 September and the year is not a leap year on either side of the boundary
- **THEN** the cutoff is the start of 18 September one year earlier

#### Scenario: Cutoff falls on 29 February
- **WHEN** the trusted-now date is 29 February of a leap year
- **THEN** the cutoff resolves to 28 February of the year one year earlier, following ordinary calendar date-minus-a-year rules

#### Scenario: A daylight-saving transition does not double-count or skip a day
- **WHEN** the year between a dose's scheduled moment and the trusted-now date spans one or more daylight-saving transitions
- **THEN** the cutoff is still exactly one local calendar year back, unaffected by the transitions

### Requirement: The purge runs inside the existing wake cycle
The app SHALL purge expired dose history as one step of its existing wake cycle, run on every wake, rather than through a separate scheduled job or a new permission. The step SHALL be idempotent: running it with nothing eligible SHALL change nothing and SHALL NOT be treated as an error.

#### Scenario: Purge runs on the daily housekeeping wake
- **WHEN** the daily housekeeping wake fires
- **THEN** any dose history past the retention cutoff is deleted as part of that same wake

#### Scenario: Purge runs on other wakes too
- **WHEN** the app wakes for a reason other than the daily housekeeping alarm (for example, a medication change or a reminder wake)
- **THEN** the purge step still runs as part of that wake

#### Scenario: Nothing to purge
- **WHEN** the purge runs and no dose row is past the retention cutoff
- **THEN** no row is deleted and the wake completes normally

#### Scenario: No new permission or network access
- **WHEN** the purge and its trusted-now guard are inspected
- **THEN** neither performs a network call nor requires any Android permission beyond what the app already declares

### Requirement: Trusted-now guards the cutoff against a tampered wall clock
The app SHALL persist a trusted-now high-water mark, paired with the boot clock reading (`SystemClock.elapsedRealtime()`) taken at the same moment, and SHALL use this trusted-now value — not the raw system wall clock — as the basis for the retention cutoff. On each wake, trusted-now SHALL advance by at most the real elapsed time the boot clock proves has passed since the previous observation, even when the wall clock has moved forward by more than that.

#### Scenario: Ordinary elapsed time
- **WHEN** a day passes between two wakes and both the wall clock and the boot clock advance by roughly one day
- **THEN** trusted-now advances by roughly one day

#### Scenario: Wall clock jumped forward
- **WHEN** the wall clock has jumped forward by a year since the last wake but the boot clock shows only a few hours have actually elapsed
- **THEN** trusted-now advances by only those few hours, not by a year

#### Scenario: A forward jump delays, but never triggers, a purge
- **WHEN** a wall-clock forward jump would otherwise place a dose past the retention cutoff
- **THEN** that dose is not purged until trusted-now, advancing only through real elapsed time, actually reaches the cutoff

#### Scenario: Wall clock moved backward
- **WHEN** the wall clock has moved backward since the last wake
- **THEN** trusted-now does not move backward, and the purge is not blocked by this alone

### Requirement: Trusted-now reseeds safely with no prior sample
When no trusted-now sample has been recorded yet, or when the previously recorded boot-clock reading is greater than the current one (indicating the device has rebooted since that sample was taken), the app SHALL treat the current wake as a fresh baseline: it SHALL record the current wall clock and boot clock as the new sample and SHALL skip the purge for that one wake.

#### Scenario: First run
- **WHEN** the app wakes for the first time with no trusted-now sample stored
- **THEN** it records a baseline sample and does not purge on that wake

#### Scenario: Reboot resets the boot clock
- **WHEN** the device has rebooted since the last recorded sample, so the current boot-clock reading is lower than the recorded one
- **THEN** the app records a fresh baseline sample and does not purge on that wake

#### Scenario: The following wake purges normally
- **WHEN** a wake after a reseed observes real elapsed time against its new baseline
- **THEN** trusted-now advances normally and the purge runs using it
