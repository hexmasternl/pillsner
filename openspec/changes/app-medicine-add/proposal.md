## Why

The medicine overview (`app-medicine-overview`) shows a large add button that leads to a titled placeholder, and the app still has no way to store anything: every medicine list on a real device is empty. Adding a medicine, with the schedules that say how much to take and when, is the first time the user gives Pillsner data, and it unlocks everything downstream: dose generation, reminders and intake tracking.

## What Changes

- Replace the Add medicine placeholder with an **Add medicine form**, reached from the + button on the medicine overview. The form collects:
  - **Name** (required).
  - **Default dose**: a quantity and a unit (for example 40 mg, 1 tablet, 5 ml). Used to prefill each schedule's amount and as the amount for as-needed use.
  - **Used since**: the date the user started or starts taking it. Defaults to today. Also serves as the anchor day for "every N days" schedules.
  - **Use until**: optional end date. Must not be before "used since".
  - **Prescribed by**: who prescribed it. Choice from General practitioner, Specialist, Pharmacist, Self, Other.
  - **Schedules**: a list of zero or more schedules, each shown with its description. A button opens the schedule editor; tapping an existing schedule reopens it for editing; each schedule can be removed. A medicine with no schedules is taken as needed.
- Add a **schedule editor screen**. Each schedule states **how much** (an amount, prefilled with the default dose) and **when**, using one of three patterns:
  - **Every N days** at one or more clock times: covers "twice a day" (N = 1, two times), "once every other day" (N = 2, one time), "three times every 3 days", and so on.
  - **Specific weekdays** at one or more clock times: covers "Mon, Wed, Fri at 08:00".
  - **Every N hours** starting from a first dose time: covers "40 mg every 12 hours from 08:00".
  The editor shows a live human-readable preview ("40 mg twice a day", "40 mg every 12 hours") so the user sees how the schedule will read on the overview, and validates before it can be saved.
- **BREAKING (model)**: a medication now owns **a list of schedules** instead of exactly one, and every schedule carries its own **amount**. The `app-medicine-overview` design argued for one schedule per medication; this change reverses that on the product owner's request so that, for example, a medicine can be 40 mg every 12 hours on weekdays and 20 mg once a day at the weekend. The as-needed shape is removed from `Schedule`; a medication with an empty schedule list is as-needed. "Every N days" gains multiple times per day, and "every N hours" gains a first-dose time so it can be generated later. Schedule descriptions now include the amount and the overview tile lists one description per schedule.
- Add a **typed dose quantity** (`Quantity` with a value and a `DoseUnit`) to the domain. This is the type the welcome screen's `UpcomingDose.amount` is intended to adopt when doses are generated; that swap is left to the scheduling change.
- Introduce **on-device persistence with Room**: medications and their schedules are stored in a local SQLite database, the repository gains a save operation, and the Room-backed repository replaces the in-memory one. The database schema is exported and a migration test harness is established at version 1 so every later schema change ships with a tested migration, as `CLAUDE.md` requires.
- Saving returns the user to the medicine overview, where the new medicine appears in the active section immediately.

## Capabilities

### New Capabilities
- `medicine-add`: The Add medicine form: fields, defaults, validation, the schedule list with add, edit and remove, saving, cancelling and discarding, accessibility.
- `schedule-editor`: The schedule editor screen: amount, the three timing patterns and their inputs, live description preview, validation, returning the schedule to the form.
- `medication-persistence`: Room-backed storage of medications and schedules on the device: schema, repository implementation, schema export, migration test harness, and the rule that nothing leaves the device.

### Modified Capabilities
- `medication-schedule-model`: `Medication` gains default dose, used since, use until and prescribed by, and holds a list of schedules; `Schedule` loses the as-needed shape, gains an amount on every shape, multiple times for every-N-days and a first-dose time for every-N-hours; the summary and human-readable description gain a count for interval schedules and include the amount; the repository contract gains a save operation and the wired implementation becomes Room-backed. This capability's spec is a delta inside the active `app-medicine-overview` change; that change MUST be archived before this one.
- `medicine-overview`: The "Medicine tile content" requirement changes so a tile shows one amount-bearing description per schedule, and "As needed" with the default dose when there are none. Same archive-order constraint as above.
- `app-navigation`: The "Add medicine destination" requirement changes from a titled placeholder to the form, and a nested **Schedule editor** destination is added inside the add-medicine flow, with the bottom bar hidden and back returning to the form. Same archive-order constraint as above.

## Impact

- **Application code (`src/`)**: `ui/medicines/add` gains the form screen, its view model and UI state, field composables (quantity input with unit picker, date fields with Material date pickers, prescriber picker), the schedule list and a `ui/medicines/schedule` editor screen with pattern selection, time list, weekday chips, interval stepper and preview. The domain gains `Quantity`, `DoseUnit`, `Prescriber`, the revised `Medication` and `Schedule`, validation rules and the revised summary. The data layer gains a Room database, `MedicationEntity`, `ScheduleEntity`, a DAO, mappers and `RoomMedicationRepository`. `AppContainer` builds the database and wires the Room repository. `ScheduleDescriptionFormatter` grows to include the amount and interval counts.
- **Dependencies** (all first party): `androidx.room:room-runtime`, `room-ktx`, `room-compiler` (via the KSP Gradle plugin, which Room's compiler requires), `room-testing` for the migration harness, `androidx.compose.material3` date and time pickers already in the Compose BOM. No third-party libraries, no network, no telemetry.
- **Depends on**: `app-welcome-screen` and `app-medicine-overview` being applied and archived first (shell, `Medicines` and `AddMedication` routes, `Medication` and `Schedule` types, in-memory repository, `AppContainer`).
- **Persistence and privacy**: a single on-device database file. Medication names and doses are never logged at info level or above. Backup behaviour is left at the platform default in this change and is called out as an open question.
- **Tests**: unit tests for `Quantity`, `Schedule` validation, the revised summary, form and editor validation, and both view models; Room DAO tests and a schema-version-1 migration harness test; Compose semantics tests for the form, the schedule editor round trip and the save-then-appear-in-overview flow.
- **README**: "Features" gains a line about adding medicines with flexible, multiple schedules once the change is archived; the technology table already lists Room.

## Non-goals

- Editing or deleting an existing medicine, and toggling active/inactive. New medicines are saved as active. Editing is the natural next change and reuses the form.
- Form, strength, notes and stock fields from the glossary. The dose unit already conveys tablets, capsules, drops and so on; strength, notes and stock arrive with their own changes.
- Generating doses from schedules, reminders and alarms. The model now carries everything the generator needs (amount, anchor date, first-dose time), but generation is its own change.
- Free-text prescriber names or contact details.
- Schedules that change over time (tapering) or depend on food and other conditions.
