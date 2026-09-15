## MODIFIED Requirements

### Requirement: Snapshots protect history
A dose SHALL store the medicine name and amount alongside its medication reference, so that a dose stays readable when the medicine it came from is gone. While a dose is pending, those stored values SHALL follow the medicine: every refresh of the planning window SHALL set the name and amount of the medicine's pending doses to the medicine's current name and the amount its schedules now call for. Once a dose has an intake — taken, skipped or missed — its stored name and amount SHALL never change again, for any reason.

#### Scenario: Medicine renamed with a dose still pending
- **WHEN** a pending dose exists for "Ibuprofen 400" and the medicine is renamed "Ibuprofen"
- **THEN** after the refresh that pending dose reads "Ibuprofen"

#### Scenario: Amount changed with a dose still pending
- **WHEN** a pending dose reads "1 tablet" and the user changes its schedule's amount to "2 tablets"
- **THEN** after the refresh that pending dose reads "2 tablets"

#### Scenario: Medicine renamed after a dose was answered
- **WHEN** a dose of "Ibuprofen 400" was recorded as taken and the medicine is later renamed "Ibuprofen"
- **THEN** the recorded dose still reads "Ibuprofen 400" with its original amount

#### Scenario: Missed dose keeps its snapshot
- **WHEN** a dose of "Ibuprofen 400" became missed and the medicine is later renamed
- **THEN** the missed dose still reads "Ibuprofen 400"

#### Scenario: Medicine deleted
- **WHEN** a medicine row is deleted from the database
- **THEN** its doses remain with their name and amount and their medication identifier becomes absent

### Requirement: Rolling two-day window
The app SHALL keep planned doses materialised for today and tomorrow in the device time zone. Refreshing the window SHALL add missing planned doses, refresh the name and amount of pending doses, and withdraw pending doses that the medicine's current schedules no longer call for. Withdrawal SHALL match a dose against the plan of **its own medicine**: a pending dose SHALL be withdrawn whenever its medicine does not plan a dose at that moment, regardless of whether another medicine happens to plan one then. A dose that has an intake SHALL never be withdrawn or altered. A dose that has already been reminded SHALL be withdrawn only when the refresh follows a change the user made to a medicine or its schedules; a refresh that follows a clock change, a time-zone change, a reboot, an app start or a reminder wake SHALL leave reminded doses in place. Refresh SHALL run at app start, whenever the medication list changes, on every reminder wake, and once per day shortly after midnight.

#### Scenario: New medicine appears
- **WHEN** a medicine with a daily 08:00 schedule is saved at 12:00
- **THEN** planned doses exist for tomorrow 08:00, and for today 08:00 as a pending dose that is already due

#### Scenario: Idempotent refresh
- **WHEN** refresh runs twice without any change
- **THEN** the set of doses is identical after both runs

#### Scenario: Schedule removed
- **WHEN** a medicine's only schedule is removed and refresh runs
- **THEN** its pending un-reminded doses in the window are deleted and its taken, skipped and missed doses remain

#### Scenario: Medicine deactivated
- **WHEN** a medicine becomes inactive and refresh runs
- **THEN** its pending un-reminded doses in the window are deleted

#### Scenario: One medicine moves to a time another medicine already uses
- **WHEN** medicine A and medicine B both have a dose planned at 08:00 and A's schedule is changed to 09:00
- **THEN** after the refresh A has a pending dose at 09:00 and none at 08:00, and B still has its pending dose at 08:00

#### Scenario: Edit withdraws a dose that was already reminded
- **WHEN** a dose at 08:00 has been reminded and is still unanswered, and the user changes the medicine's schedule so that no dose is planned at 08:00
- **THEN** after the refresh the 08:00 dose no longer exists

#### Scenario: Clock change does not withdraw a reminded dose
- **WHEN** a dose at 08:00 has been reminded and is still unanswered, and the device time zone changes so that the medicine now plans its dose at a different instant
- **THEN** the reminded 08:00 dose still exists and is not reminded a second time

#### Scenario: Answered dose survives an edit that drops its moment
- **WHEN** a dose at 08:00 was recorded as taken and the user changes the medicine's schedule so that no dose is planned at 08:00
- **THEN** the taken dose at 08:00 still exists with its outcome, name and amount unchanged

#### Scenario: Day rollover
- **WHEN** the clock passes midnight and the daily refresh runs
- **THEN** planned doses for the new tomorrow exist

## ADDED Requirements

### Requirement: Refresh reports what it withdrew
Refreshing the window SHALL report the identifiers of the doses it withdrew, so that a caller can take down anything it has already shown the user for them. A refresh that withdraws nothing SHALL report an empty result.

#### Scenario: Withdrawn doses are reported
- **WHEN** a refresh withdraws two pending doses
- **THEN** it reports the identifiers of exactly those two doses

#### Scenario: Nothing withdrawn
- **WHEN** a refresh adds a dose and withdraws none
- **THEN** it reports no identifiers
