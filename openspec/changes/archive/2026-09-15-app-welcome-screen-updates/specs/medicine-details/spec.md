## MODIFIED Requirements

### Requirement: Editing preserves history
Saving an edit SHALL NOT alter any recorded intake. Doses that are still pending SHALL follow the edited medicine: their name and amount SHALL become the medicine's current name and amount, and doses the edited schedules no longer call for SHALL be withdrawn, including a dose the user has already been reminded about. Withdrawing a dose SHALL remove any reminder showing for it. A dose that already has an outcome — taken, skipped or missed — SHALL be left exactly as it is.

#### Scenario: Rename keeps past intakes
- **WHEN** a dose of "Ibuprofen 400" was recorded as taken yesterday and the user renames the medicine to "Ibuprofen"
- **THEN** yesterday's intake still reads "Ibuprofen 400" with its original amount and time

#### Scenario: Rename reaches pending doses
- **WHEN** a medicine named "Ibuprofen 400" has a pending dose today and the user renames it to "Ibuprofen" and saves
- **THEN** that pending dose reads "Ibuprofen", on the Welcome screen and in its reminder

#### Scenario: Schedule change replans future doses
- **WHEN** a medicine has a planned, un-reminded dose at 20:00 today and the user changes its only schedule from 08:00 and 20:00 to 09:00 and 21:00
- **THEN** after saving, the planned dose at 20:00 is gone and a planned dose at 21:00 exists, while any dose already taken, skipped or missed today is unchanged

#### Scenario: Edit withdraws a reminder that no longer applies
- **WHEN** a reminder for today's 08:00 dose is showing unanswered and the user changes the medicine's schedule so that it no longer has an 08:00 dose, then saves
- **THEN** that dose is withdrawn and its reminder is removed

#### Scenario: Reminded dose that survives the edit picks up the new amount
- **WHEN** a reminder for today's 08:00 dose is showing unanswered and the user changes the medicine's amount while keeping 08:00, then saves
- **THEN** that dose remains pending at 08:00 and shows the new amount

#### Scenario: Edit does not disturb another medicine
- **WHEN** two medicines both have a pending dose at 08:00 and the user moves one of them to 09:00
- **THEN** the other medicine's 08:00 dose is untouched
