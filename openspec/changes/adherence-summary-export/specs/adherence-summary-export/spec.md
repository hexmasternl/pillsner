## ADDED Requirements

### Requirement: Report content mirrors the Usage history screen exactly
The exported report SHALL be built from the same medicine, period and figures the Usage history screen currently shows: the medicine's name, its schedule in plain language, the period label, the scheduled/taken/skipped/missed/unanswered counts and the adherence percentage. The report generation MUST NOT perform its own adherence calculation; it SHALL reuse the values already computed for the screen.

#### Scenario: Figures match
- **WHEN** the Usage history screen shows 92% adherence for "1 month" and the user exports a report for that period
- **THEN** the report states 92% adherence and the same scheduled/taken/skipped/missed/unanswered counts shown on screen

#### Scenario: Period follows the screen
- **WHEN** the screen has "3 months" selected and the user starts an export
- **THEN** the report covers the "3 months" period without asking the user to choose a period again

### Requirement: Dose-level detail is opt-in and off by default
The export flow SHALL offer an "Include individual doses" choice, off by default, before generating the report. When off, the report SHALL contain only the summary figures. When on, the report SHALL additionally contain one row per dose in the period: its due date and time, the date and time it was answered if it was, and its outcome.

#### Scenario: Default export has no dose-level detail
- **WHEN** the user starts an export and does not change the "Include individual doses" choice
- **THEN** the generated report contains only the summary figures and no per-dose rows

#### Scenario: Opting in adds the appendix
- **WHEN** the user turns on "Include individual doses" before exporting
- **THEN** the generated report includes a per-dose row for every dose counted in the period's figures

### Requirement: PDF and CSV are the two export formats
The export flow SHALL let the user choose between a PDF report, preselected as the default, and a CSV file, before generating and sharing it. Both formats SHALL be generated entirely on-device using platform APIs, without a network request. The CSV file SHALL contain whatever level of detail (summary only, or summary plus per-dose rows) the user chose for that export.

#### Scenario: PDF is the default choice
- **WHEN** the export dialog opens
- **THEN** PDF is the preselected format

#### Scenario: CSV reflects the same detail choice
- **WHEN** the user chooses CSV with "Include individual doses" on
- **THEN** the generated CSV file contains the per-dose rows alongside the summary figures

### Requirement: The generated file is handed to the Android share sheet
Once generated, the report file SHALL be shared through an `ACTION_SEND` intent resolved by the system share sheet, exposing the file via a scoped `FileProvider` with read-only, time-limited access. Pillsner MUST NOT transmit the file anywhere itself; the user chooses the destination app, printer or save location from the share sheet.

#### Scenario: Share sheet appears
- **WHEN** report generation finishes
- **THEN** the system share sheet opens with the generated file attached and no network request has been made

#### Scenario: Only the export directory is exposed
- **WHEN** the granted URI permission is inspected
- **THEN** it grants read-only access to the single generated file and nothing else under the app's storage

### Requirement: Exported files are disposable cache, not managed data
Generated report files SHALL be written to the app's private cache storage and MUST NOT be presented anywhere in the app as a list of past exports to browse, rename or delete. Every export SHALL write a new, distinctly named file rather than overwrite a previous one.

#### Scenario: No export history screen
- **WHEN** the app's navigation and screens are inspected
- **THEN** there is no screen listing previously generated reports

#### Scenario: Repeated exports do not collide
- **WHEN** the user exports a report for the same medicine and period twice in a row
- **THEN** two distinct files are written and the first is left untouched
