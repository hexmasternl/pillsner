# Pillsner

**Your partner in taking your pills.**

Pillsner is a native Android reminder app that helps you take your medication on time, every time. The name is a play on *Pills* and *Partner*: the app is meant to be the reliable companion that taps you on the shoulder when a dose is due, keeps track of what you have taken, and stays out of your way the rest of the time.

> **Project status: early development.** The repository currently holds the project scaffolding and the spec-driven planning workflow. The Android app itself is being built in the `src` folder. Expect the feature list below to describe intent rather than shipped functionality until a first release is tagged.

---

## Why Pillsner

Missing a dose, or taking one twice because you forgot you already did, is a common and frustrating problem. Most people do not want a full health platform for this. They want a small, trustworthy app that:

- reminds them at the right moment, reliably, even when the phone is idle;
- makes confirming a dose a one-tap action;
- shows at a glance what is due today and what has already been taken;
- keeps their medication data private and on their own device.

Pillsner is built around exactly those needs and nothing more.

## Features

The following capabilities define the scope of the app. Items are being delivered incrementally through the change proposals in the `openspec` folder.

**Medication management**
- Add a medication with a name, dosage, form (tablet, capsule, drops, and so on) and optional notes.
- Edit, pause or archive a medication without losing its history.
- Track remaining stock and get a heads-up when a refill is due.

**Schedules and reminders**
- Flexible schedules: fixed times of day, every N hours, specific weekdays, or as-needed medication without a schedule.
- Reliable local notifications that fire on time, including when the device is dozing.
- Snooze a reminder for a short while, or skip a dose deliberately and have that recorded as a skip rather than a miss.

**Intake tracking**
- Confirm a dose straight from the notification or from the app.
- A daily overview showing what is due, what is taken, what was skipped and what was missed.
- A history view so you or a caregiver can see adherence over time.

**Privacy by default**
- All data lives on the device. There is no account, no cloud sync and no analytics unless explicitly added and clearly disclosed in a future release.

## Technology

Pillsner is a native Android application written in Kotlin.

| Area | Choice |
| --- | --- |
| Language | Kotlin |
| Platform | Android (native) |
| UI toolkit | Jetpack Compose with Material 3 |
| Architecture | Single-activity, MVVM style with unidirectional data flow |
| Persistence | Room (SQLite) on the device |
| Scheduling | Android alarm and notification APIs for exact, reliable reminders |
| Build system | Gradle with the Kotlin DSL |
| IDE | Android Studio (latest stable) |

Any change to this table should go through the spec-driven workflow described below, so that the reasoning is recorded alongside the decision.

## Repository layout

| Path | Purpose |
| --- | --- |
| `src/` | The Android application. Open this folder as the project in Android Studio. |
| `openspec/` | Spec-driven planning: `specs/` holds the agreed behaviour of the app, `changes/` holds in-progress change proposals, and `changes/archive/` holds completed ones. |
| `.claude/` | Configuration for AI-assisted development (skills and slash commands for the OpenSpec workflow). |
| `CLAUDE.md` | Working instructions for AI coding assistants contributing to this repository. |
| `LICENSE` | MIT license. |

## Getting started

### Prerequisites

- Android Studio, latest stable release, with the Android SDK installed.
- A JDK compatible with the Android Gradle Plugin version used by the project (Android Studio bundles a suitable one).
- An Android device or emulator. Because the app schedules exact alarms and posts notifications, testing on a physical device gives the most realistic picture of reminder reliability.

### Building and running

1. Clone the repository.
2. Open the `src` folder in Android Studio and let Gradle sync finish.
3. Select a device or emulator and press Run.

From a terminal inside the `src` folder, the usual Gradle wrapper tasks apply: `assembleDebug` produces a debug build, `test` runs the unit tests, and `connectedAndroidTest` runs the instrumented tests on an attached device.

### Permissions the app asks for

Pillsner needs permission to post notifications and to schedule exact alarms. Both are essential to its purpose. It does not request location, contacts, network or any other permission that is not needed to remind you of a dose.

## Development workflow

Pillsner uses a spec-driven workflow powered by [OpenSpec](https://github.com/Fission-AI/OpenSpec). Instead of jumping straight into code, every meaningful change starts as a proposal that captures **what** is changing and **why**, followed by a design that captures **how**, and a task list that breaks the work into steps.

The typical loop is:

1. **Propose** a change. A new folder appears under `openspec/changes/` with a proposal, a design and a task list.
2. **Apply** the change by working through the tasks and updating the code in `src`.
3. **Archive** the change once it is complete. The relevant specs in `openspec/specs/` are updated to reflect the new agreed behaviour, and the change moves to `openspec/changes/archive/`.

The `openspec/specs/` folder is therefore the living description of how Pillsner behaves. When the code and a spec disagree, either the code has a bug or the spec needs a change proposal. Never silently drift.

## Contributing

Contributions are welcome. To keep the project coherent:

- Start non-trivial work with a change proposal rather than a surprise pull request.
- Keep the app small and focused. A feature that does not directly help someone take their medication correctly probably belongs in a different app.
- Preserve the privacy stance. Do not introduce network access, third-party SDKs or telemetry without an accepted proposal that explains the trade-off.
- Write tests for scheduling and intake logic. Reminder timing is the one thing this app must never get wrong.
- Use clear, conventional commit messages that describe the intent of the change.

Bug reports and feature ideas can be filed as GitHub issues on this repository.

## License

Pillsner is released under the MIT License. See the [LICENSE](LICENSE) file for details.

Copyright (c) 2026 HexMaster
