# dose-history-retention Specification

## Purpose
TBD - created by archiving change dose-history-retention. Update Purpose after archive.

## Requirements
### Requirement: Dose history purge
The app SHALL delete `Dose` rows whose scheduled moment is more than one calendar year before
trusted-now, as one step of its existing daily housekeeping wake. Only `Dose` rows are ever
deleted by this purge; `Medication` and `Schedule` rows and every dose still inside the rolling
two-day pending window (`dose-records`, "Rolling two-day window") are never touched. The cutoff
SHALL be computed as trusted-now's local date, in the system's current time zone read at the
moment of computation, minus one year, using local calendar-date arithmetic so month lengths, leap
days and daylight-saving transitions need no special case.

#### Scenario: A dose over a year old is purged
- **WHEN** trusted-now is 20 September 2026 and a dose is recorded with a scheduled moment of
  1 August 2025
- **THEN** that dose is deleted by the next housekeeping wake

#### Scenario: A dose just under a year old is kept
- **WHEN** trusted-now is 20 September 2026 and a dose is recorded with a scheduled moment of
  1 October 2025
- **THEN** that dose still exists after the next housekeeping wake

#### Scenario: Leap day cutoff
- **WHEN** trusted-now is 1 March of the year after a leap year and a dose was scheduled on
  29 February of the leap year
- **THEN** that dose is purged, because one calendar year before 1 March is 1 March, which is after
  29 February of the leap year

#### Scenario: Nothing eligible is a no-op
- **WHEN** the housekeeping wake runs and every stored dose is less than a year old
- **THEN** no dose row is deleted and the wake completes normally

#### Scenario: Medication and schedule rows are never touched
- **WHEN** the purge deletes one or more dose rows for a medicine
- **THEN** that medicine's `Medication` row and its `Schedule` rows still exist unchanged

#### Scenario: Time zone change is reflected immediately
- **WHEN** the device's system time zone changes between two housekeeping wakes
- **THEN** the next purge computes its one-year-ago cutoff using the new zone, not the zone in
  effect at any earlier wake

### Requirement: Trusted-now clock guard
The app SHALL maintain a persisted "trusted now" high-water mark, anchored to the monotonic boot
clock, that the purge uses instead of the raw system wall clock as the basis for trusted-now.
Trusted-now SHALL advance, on each observation, by no more than the real elapsed time the boot
clock recorded since the previous observation, regardless of how far the wall clock has moved
forward in that interval, so that moving the system clock forward can only delay a purge, never
trigger one early. Moving the system clock backward is not treated as tampering and SHALL NOT be
guarded against; it can only delay a purge further, never remove data early. The guard SHALL
require no Android permission beyond what the app already holds and SHALL make no network request.

A first observation with no persisted sample yet SHALL NOT trust the raw wall clock outright: when
the app already has an *answered* dose in storage, the most recent moment any answered dose was
stored SHALL be used as independent evidence of how far real time has already reached, and the seed
SHALL be clamped down to whichever of the two readings is earlier. A still-pending dose's stored
moment MUST NOT be used as this evidence, since it can be a projection of a future moment rather
than something that has already happened. This evidence MUST be drawn only from what existed before
the current wake began; a dose the current wake itself stores MUST NOT be used as evidence for that
same wake's own observation. Only when neither a persisted sample nor any qualifying answered dose
exists SHALL the seed be taken from the raw wall clock, with no purge run on that observation, since
there is nothing yet to validate that reading against and nothing yet a wrong seed could delete.

#### Scenario: Ordinary elapsed time
- **WHEN** ten real minutes pass between two observations and the wall clock also advanced by ten
  minutes
- **THEN** trusted-now advances by ten minutes

#### Scenario: Forward clock jump is capped
- **WHEN** the boot clock records ten minutes between two observations but the wall clock has been
  moved forward by 400 days in that same interval
- **THEN** trusted-now advances by only ten minutes, not 400 days

#### Scenario: Backward clock move does not regress trusted-now
- **WHEN** the wall clock is moved backward before an observation
- **THEN** trusted-now does not decrease, and no purge is triggered earlier than it otherwise would
  have been

#### Scenario: Reboot does not reseed from the raw wall clock
- **WHEN** the device reboots between two observations, so the boot clock has reset
- **THEN** trusted-now is left exactly where it was at the last observation, and is not replaced by
  a fresh reading of the (possibly tampered) wall clock

#### Scenario: First run with no dose history seeds trusted-now but does not purge yet
- **WHEN** the app has no persisted trusted-now sample and no dose has ever been stored
- **THEN** a first sample is seeded from the current wall clock, and the housekeeping wake that
  observes it does not run a purge, since there is nothing yet to validate that reading against and
  nothing yet a wrong seed could delete

#### Scenario: First run with existing dose history clamps a tampered seed and purges immediately
- **WHEN** the app has no persisted trusted-now sample, but an answered dose already exists from
  before this observation, and the wall clock now reads far later than that dose's stored moment
- **THEN** the seed is clamped to that stored moment rather than the tampered wall clock, and the
  purge runs on this same wake using the clamped value

#### Scenario: A still-pending dose is never used as the floor
- **WHEN** the app has no persisted trusted-now sample and no answered dose, but a still-pending
  dose exists whose own stored moment projects into the future
- **THEN** that pending dose's stored moment is not used to clamp the seed, and this observation
  behaves as though no dose history existed at all

#### Scenario: A dose the current wake itself just stored is never used as its own evidence
- **WHEN** the current wake stores a new dose before this observation runs
- **THEN** that newly stored dose is not used as evidence for this same observation; only doses that
  existed before the current wake began may be

#### Scenario: A later wake purges normally after the first-run seed
- **WHEN** a subsequent housekeeping wake observes real elapsed time since the first-run seed
- **THEN** trusted-now advances from the seed by that elapsed time and the purge runs normally

#### Scenario: No permission or network access
- **WHEN** the app is built and installed
- **THEN** the trusted-now guard requires no `INTERNET` permission and makes no network request

### Requirement: Purge runs without costing a reminder
The dose-history purge SHALL run as the last step of the existing wake cycle, inside its own
failure boundary. A purge failure SHALL be logged and MUST NOT prevent the wake's reminder work or
its alarm reconciliation from completing normally. The purge's failure boundary MUST NOT catch a
cancellation that ends the wake's own time budget; such a cancellation SHALL propagate so the wake
is retried per `reminder-scheduling`'s "A wake that does not complete is retried" requirement,
rather than the timed-out wake being reported as complete.

#### Scenario: Purge failure does not block reminders
- **WHEN** a wake has due doses to post and the purge step throws
- **THEN** the due doses are still posted, the purge failure is logged, and the next alarm is still
  reconciled

#### Scenario: A wake that times out during the purge is still retried
- **WHEN** the wake's overall time budget expires while the purge step is running
- **THEN** the wake is reported as timed out, not completed, and a retry is armed exactly as it
  would be for a timeout at any other step

#### Scenario: A purge that finds nothing to delete does not block the wake either
- **WHEN** the purge step runs and no dose qualifies for deletion
- **THEN** the wake completes exactly as it would have without this step
