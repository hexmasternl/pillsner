## ADDED Requirements

### Requirement: Usage chart value axis
The usage chart SHALL show a value axis to the left of its bars: the busiest bucket's scheduled count at the top, aligned with the tallest bar, and zero at the bottom, aligned with the baseline every bar rises from. Each axis label SHALL state its unit so it reads correctly on its own.

#### Scenario: The axis states the busiest bucket's value
- **WHEN** the busiest bucket in the period has 4 scheduled doses
- **THEN** the axis shows "4 doses" at the top and "0 doses" at the bottom

#### Scenario: The axis does not add a bar-tapping interaction
- **WHEN** the usage chart and its axis are inspected
- **THEN** neither the chart nor the axis records, changes or removes anything, and no tap reveals additional information beyond what each bar already announces

### Requirement: Timing accuracy chart value axis
The timing accuracy chart SHALL show a value axis to the left of its bars: the busiest bucket's average number of minutes at the top, aligned with the tallest bar, and zero at the bottom, aligned with the baseline every bar rises from. Each axis label SHALL state that it is a number of minutes so it reads correctly on its own.

#### Scenario: The axis states the busiest bucket's average
- **WHEN** the busiest bucket in the period averages 22 minutes
- **THEN** the axis shows "22 minutes" at the top and "0 minutes" at the bottom

#### Scenario: No taken dose anywhere in the period
- **WHEN** the timing accuracy chart is omitted because nothing was taken in the period
- **THEN** its axis is omitted along with it
