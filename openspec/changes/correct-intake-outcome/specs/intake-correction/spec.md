# Spec Delta

## Purpose

Lets the user put right a dose outcome that was recorded wrongly, such as a mis-tapped reminder answer or a dose taken but never confirmed, while keeping stock, adherence and the distinction between skipped and missed doses truthful.

## ADDED Requirements

### Requirement: Which doses can be corrected
A dose SHALL be correctable when it has a recorded outcome (taken, skipped or missed) and its scheduled moment falls within the last 14 days: from the start of the local day 13 days before today, in the device time zone, up to the present moment. A dose with no recorded outcome MUST NOT be correctable, and neither may a dose scheduled earlier or later than that window.

#### Scenario: A recent taken dose
- **WHEN** a dose scheduled three days ago was recorded taken
- **THEN** it can be corrected

#### Scenario: Older than 14 days
- **WHEN** today is 20 October and a dose scheduled on 6 October was recorded skipped
- **THEN** it cannot be corrected

#### Scenario: First day of the window
- **WHEN** today is 20 October and a dose scheduled at 00:30 on 7 October was recorded missed
- **THEN** it can be corrected

#### Scenario: Not answered yet
- **WHEN** a dose scheduled an hour ago has no recorded outcome
- **THEN** it cannot be corrected and is still answered from its reminder or the dose detail screen

### Requirement: "I took it" records taken at the scheduled moment
Correcting a dose with "I took it" SHALL record its outcome as taken, with the dose's scheduled moment as the taken moment. Choosing it for a dose that is already recorded taken SHALL change nothing.

#### Scenario: Missed corrected to taken
- **WHEN** a dose scheduled at 08:00 yesterday lapsed as missed and the user corrects it with "I took it"
- **THEN** its outcome is taken with a taken moment of 08:00 yesterday

#### Scenario: Skipped corrected to taken
- **WHEN** a dose recorded skipped is corrected with "I took it"
- **THEN** its outcome is taken with its scheduled moment as the taken moment

#### Scenario: Already taken
- **WHEN** a dose recorded taken at 08:12 is saved with "I took it"
- **THEN** its outcome stays taken at 08:12 and no stock changes

### Requirement: "I didn't take it" records skipped
Correcting a dose with "I didn't take it" SHALL record its outcome as skipped, with the moment of the correction as its recorded moment. It MUST NOT record missed, which remains reserved for a dose that lapsed unanswered. Choosing it for a dose already recorded skipped SHALL change nothing. Choosing it for a missed dose SHALL record skipped.

#### Scenario: Taken corrected to not taken
- **WHEN** a dose recorded taken is corrected with "I didn't take it"
- **THEN** its outcome is skipped, and it counts as skipped rather than missed in the usage history

#### Scenario: Missed corrected to not taken
- **WHEN** a dose that lapsed as missed is corrected with "I didn't take it"
- **THEN** its outcome is skipped

### Requirement: A corrected dose is marked as corrected
A dose whose outcome was changed by a correction SHALL be marked as corrected, and the mark SHALL be kept if the dose is corrected again. A save that changed nothing MUST NOT mark the dose.

#### Scenario: Marked after a change
- **WHEN** a missed dose is corrected to taken
- **THEN** the dose is marked as corrected

#### Scenario: No change, no mark
- **WHEN** a taken dose is saved with "I took it"
- **THEN** the dose is not marked as corrected

### Requirement: Correcting to taken uses up stock
When a dose is corrected to taken, the dose's amount SHALL be taken from stock exactly as for a dose recorded taken from its reminder, by the same first-expiry-first-out rules and in the same transaction as the outcome change. The low-stock and expiry-at-use warnings SHALL be evaluated afterwards as for any taken dose. A medicine with no stock recorded SHALL be unaffected.

#### Scenario: Stock decreases
- **WHEN** a medicine has one batch of 20 tablets and a missed 1-tablet dose is corrected to taken
- **THEN** the batch holds 19 tablets

#### Scenario: Low-stock warning follows
- **WHEN** correcting a dose to taken leaves stock below the projected weekly usage and the low-stock acknowledgement is unset
- **THEN** the low-stock warning is shown

### Requirement: Taking a dose records what it drew from stock
Whenever a dose is recorded taken and stock is used up, the app SHALL record, for each batch that gave something, how much that batch gave in the batch's own unit, including any whole-pill rounding. The record is kept on the device only and is removed with the dose or with the batch.

#### Scenario: A dose spanning two batches
- **WHEN** a 2-tablet dose is taken from a batch with 1 tablet left and a second batch with 30
- **THEN** the app records 1 tablet drawn from each batch for that dose

### Requirement: Correcting away from taken returns stock
When a dose recorded taken is corrected to skipped, every amount recorded as drawn for that dose SHALL be added back to the batch it came from, in the same transaction as the outcome change, and the draw records SHALL then be removed. Stock with no draw record, because the dose was taken before draws were recorded or because its batch has since been removed, SHALL be left unchanged, and the correction MUST still succeed.

#### Scenario: Stock is returned to the right batch
- **WHEN** a dose drew 1 tablet from batch A and 1 from batch B and is corrected with "I didn't take it"
- **THEN** batch A and batch B each hold one tablet more than before the correction

#### Scenario: Whole-pill rounding is returned in full
- **WHEN** a 40 mg dose drew one whole 50 mg tablet and is corrected to skipped
- **THEN** that batch gets one whole tablet back

#### Scenario: Batch removed since
- **WHEN** a taken dose drew from a batch the user has since removed
- **THEN** the dose is corrected to skipped and no batch changes

#### Scenario: Taken before draws were recorded
- **WHEN** a dose taken before the app recorded draws is corrected to skipped
- **THEN** the dose is corrected to skipped and no batch changes

### Requirement: A correction never overwrites a change the user did not see
A correction SHALL only be applied while the dose's outcome is still the one the correction was made against. If the outcome changed in the meantime, the correction MUST NOT be applied and the user SHALL be told that the dose changed and shown its current outcome.

#### Scenario: Answered elsewhere in between
- **WHEN** the correction sheet is open for a dose shown as missed and its outcome changes to skipped before the user saves
- **THEN** saving applies nothing and a message states that the dose was updated in the meantime

### Requirement: Reminders and the watch are not affected
A correction SHALL NOT schedule, cancel or post a reminder, SHALL NOT change any pending dose, and SHALL NOT change what is published to the watch. The missed rule MUST NOT change the outcome of a corrected dose.

#### Scenario: Missed rule leaves a correction alone
- **WHEN** a missed dose is corrected to skipped and the missed check runs afterwards
- **THEN** the dose stays skipped

#### Scenario: No reminder effect
- **WHEN** a dose from yesterday is corrected
- **THEN** no alarm is scheduled or cancelled and no notification is posted or removed

### Requirement: No sensitive logging on the correction path
The correction path SHALL NOT log a medicine name, a dose amount or a stock amount at info level or above.

#### Scenario: Log inspection
- **WHEN** a correction is made with logging enabled
- **THEN** no log entry at info level or above contains the medicine's name, the dose's amount or any stock amount
