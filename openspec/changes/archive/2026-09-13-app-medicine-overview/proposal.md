## Why

The Medicines destination introduced by `app-welcome-screen` is a titled placeholder, so a user has no way to see which medicines Pillsner knows about, let alone add one. Before schedules can generate doses and reminders can fire, the user needs a single place that answers "what am I taking, how often, and what have I stopped taking?" and offers the obvious way in: add a medicine.

## What Changes

- Replace the placeholder Medicines destination with the **medicine overview screen**. It lists every medicine the user has entered, split into two sections: **active** medicines at the top and **inactive** medicines below. Active means the user is currently taking it and its schedule produces doses; inactive means the user has stopped it but kept it for reference and history.
- Each medicine is shown as a **tile** with the medicine name and a **human-readable description of its schedule**, for example "Twice a day", "Once every other day", "Every 8 hours", "Once a day on Mon, Wed, Fri" or "As needed". Descriptions are derived from the schedule rule, come from string resources and are ready for translation.
- Medicines are ordered by name within each section. Inactive tiles are visually de-emphasised and announced as inactive to screen readers.
- Add **empty states**: a friendly message with a hint towards the add button when the user has no medicines at all. The inactive section is hidden when it has nothing to show.
- Add a large **floating action button** (FAB) at the bottom right of the screen that starts adding a new medicine. Tapping it navigates to an **Add medicine** destination. In this change that destination is a titled placeholder with the bottom bar hidden; the actual form is delivered by a separate change.
- Introduce the **domain contract** the overview needs: a `Medication` model with an active flag, a `Schedule` model that expresses fixed daily times, weekday selections, day and hour intervals and as-needed use, a `MedicationRepository` interface exposing medications as a `Flow`, and a pure domain `ScheduleSummary` that the UI maps to the human-readable description. Until persistence lands, the wired implementation holds an empty in-memory list and a debug preview implementation supplies sample data.

## Capabilities

### New Capabilities
- `medicine-overview`: The Medicines screen: two sections (active, inactive) of medicine tiles showing name and a human-readable schedule description, ordering, empty states, live updates, accessibility, and the add-medicine FAB.
- `medication-schedule-model`: The domain model for a medication and its schedule, the repository contract that supplies them, and the rules that turn a schedule into a summary suitable for a human-readable description.

### Modified Capabilities
- `app-navigation`: The "Placeholder Medicines and Settings destinations" requirement changes so that Medicines shows the overview screen rather than a title-only placeholder (Settings keeps its placeholder). A new nested **Add medicine** destination is added, reached from the overview FAB, with the bottom navigation bar hidden and back returning to Medicines. This capability's spec is currently a delta inside the active `app-welcome-screen` change; that change MUST be archived before this one.

## Impact

- **Application code (`src/`)**: the `ui/medicines` feature area gains the overview screen, its tiles, section headers, empty state, view model and UI state, plus a `ScheduleDescriptionFormatter` that maps a domain summary to string resources. A `ui/medicines/add` placeholder screen and an `AddMedication` route are added to the navigation host. The domain layer gains `Medication`, `MedicationId`, `Schedule`, `ScheduleSummary` and `MedicationRepository`; the data layer gains an `InMemoryMedicationRepository` (empty) and a debug-only preview repository. `AppContainer` exposes the new repository and view model factory.
- **Dependencies**: none added. Everything uses the Compose, Material 3, Navigation and Lifecycle artifacts already in the version catalog. No network, no telemetry.
- **Depends on**: `app-welcome-screen` (shell, bottom bar, Medicines route, `AppContainer`) being applied first. The `app-login` change is unaffected.
- **Persistence**: none in this change. The repository interface is designed so the later Room-backed implementation replaces the in-memory one without touching the UI.
- **Tests**: unit tests for schedule summarisation (every schedule shape, singular and plural counts, every-other-day), for the view model's partitioning and ordering, and for the description formatter; Compose semantics tests for the two sections, empty states, inactive announcement and the FAB navigation.
- **README**: the "Features" section gains a line about the medicine overview once the change is archived.

## Non-goals

- The add-medicine form itself, editing or deleting a medicine, and toggling active/inactive. The overview only reads.
- Showing dose amount, form, strength, stock or notes on the tile. The tile shows name and schedule description only.
- Room persistence and migrations. These arrive with the add-medicine change, which is the first to write data.
- Opening a medicine detail screen from a tile. Tiles are informational in this change.
- Generating doses from the schedule model. The model is defined here so descriptions can be derived; dose generation belongs to the scheduling change.
