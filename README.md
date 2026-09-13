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
- A medicine overview listing what you take, split into active and inactive medicines, each with a plain-language description of its schedule, and a large add button to enter a new one. Swipe a tile sideways to activate or deactivate that medicine; nothing is ever deleted.
- Add a medication with a name, a default dose and its unit (mg, ml, tablet, drop, and so on), the dates you take it between, and who prescribed it.
- Give a medicine as many schedules as it needs, each with its own amount: 40 mg every 12 hours on top of 20 mg once a day at the weekend.
- Tap a medicine to open it and change anything: its name, its dose, its dates, who prescribed it, its schedules, and whether you are currently taking it.
- **A medicine is never deleted.** Stopping one deactivates it; the medicine, its schedules and every dose it ever produced stay on your device. Editing a medicine never rewrites what you already took: doses you have answered keep the name and amount they were taken under, and only what is still ahead of you follows the change.
- Track remaining stock and get a heads-up when a refill is due.

**Schedules and reminders**
- Flexible schedules: fixed times of day, every N hours, specific weekdays, or as-needed medication without a schedule.
- Reliable local notifications that fire on the exact minute, including when the device is dozing, and that survive a reboot, an app update, a clock change and a move to another time zone.
- Answer a reminder in one tap, without opening the app: **I took it**, **Not yet** (a 15-minute snooze) or **Not going to**. A dose you never answer becomes missed when the next one is due, or 24 hours later, whichever comes first.
- The same reminder, with the same three answers, appears on a paired Wear OS watch.

**Home**
- A welcome screen showing your upcoming doses, soonest first, so you can see at a glance what needs taking next.

**Intake tracking**
- Confirm a dose straight from the notification or from the app.
- A daily overview showing what is due, what is taken, what was skipped and what was missed.
- A history view so you or a caregiver can see adherence over time.

**Language**
- Pillsner is available in English and Dutch, and follows your phone's language on its own. You can override it in Settings; the new language appears the next time you start the app, and Pillsner says so until you do.
- Everything follows the chosen language, not only the screens: dates, times, weekday names, decimal separators, how names are sorted, and the reminder that arrives while the app is closed.

**Privacy by default**
- All data lives on the device. There is no account, no cloud sync and no analytics unless explicitly added and clearly disclosed in a future release.
- An optional app lock protects the app with a PIN and, once set up, biometric unlock. Screenshots and the recent apps thumbnail are hidden while it is on.
- You can change your PIN in Settings without turning the lock off. Changing the PIN, turning biometric unlock off and turning the lock off each ask you to confirm it is you first — with your fingerprint or face where you have one, and with the PIN itself for turning the lock off. Each confirmation is good for that one change.
- On the lock screen a reminder can show only "Time for your medicine", never the name or the amount, whenever your phone is set to hide sensitive notifications.

## Permissions

Pillsner asks for as little as it can, and for nothing that sends data anywhere. It declares **no internet permission at all**.

| Permission | Why |
| --- | --- |
| `POST_NOTIFICATIONS` | A reminder is a notification. Without it Pillsner cannot remind you of anything, and the Home screen says so. |
| `USE_EXACT_ALARM` (Android 13+), `SCHEDULE_EXACT_ALARM` (Android 12) | A dose due at 08:00 has to be announced at 08:00. Android reserves exact alarms for apps whose core function is alarms or reminders; that is exactly what Pillsner is. Without it reminders fall back to a ten-minute window and the Home screen warns you. |
| `RECEIVE_BOOT_COMPLETED` | Restarting the phone clears every pending alarm, so Pillsner has to set its own again. |

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
| Design | `docs/design-system.md`: brand colour schemes for light and dark, bundled Montserrat and Raleway, Material 3 tokens; no dynamic colour |
| Build system | Gradle with the Kotlin DSL, versions pinned in a version catalog (see Toolchain below) |
| IDE | Android Studio (latest stable) |

Any change to this table should go through the spec-driven workflow described below, so that the reasoning is recorded alongside the decision.

## Repository layout

| Path | Purpose |
| --- | --- |
| `src/` | The Android application. Open this folder as the project in Android Studio. |
| `openspec/` | Spec-driven planning: `specs/` holds the agreed behaviour of the app, `changes/` holds in-progress change proposals, and `changes/archive/` holds completed ones. |
| `docs/` | The design system (`design-system.md`) and its visual companion (`design-system.html`): colours, typography, components and accessibility rules for the app. |
| `.claude/` | Configuration for AI-assisted development: skills and slash commands for the OpenSpec workflow, plus the design agent and UI skills that enforce the design system. |
| `CLAUDE.md` | Working instructions for AI coding assistants contributing to this repository. |
| `LICENSE` | MIT license. |

## Getting started

### Prerequisites

- Android Studio, latest stable release, with the Android SDK installed (SDK Platform 37 and Build Tools 36.0.0).
- JDK 21. Android Studio bundles it; on the command line, point `JAVA_HOME` at a JDK 21 installation.
- An Android device or emulator. Because the app schedules exact alarms and posts notifications, testing on a physical device gives the most realistic picture of reminder reliability.

### Toolchain

The project pins every tool and library version in `src/gradle/libs.versions.toml`. The policy is: the newest stable release of each, or the newest long-term-support release where the tool has one, never a pre-release. The baseline below was set on 11 September 2026 by the `app-welcome-screen` change and is re-checked whenever a change touches the catalog.

| Component | Version |
| --- | --- |
| JDK (Gradle runtime and JVM toolchain) | 21 (LTS) |
| Gradle wrapper | 9.7.1 |
| Android Gradle Plugin | 9.4.0 |
| Kotlin (with Compose compiler and serialization plugins) | 2.4.20 |
| KSP | 2.3.12 |
| `compileSdk` / `targetSdk` | 37 (Android 17) |
| `minSdk` | 26 (phone), 30 (Wear OS) |
| Jetpack Compose BOM | 2026.09.00 (Compose UI/Foundation/Runtime 1.12.1, Material 3 1.4.0) |
| AndroidX Navigation Compose | 2.10.1 |
| AndroidX Lifecycle | 2.11.0 |
| AndroidX Activity Compose | 1.13.0 |
| AndroidX Core | 1.19.0 |
| Room | 2.8.5 |
| DataStore Preferences | 1.2.1 |
| Biometric | 1.1.0 |
| Fragment (host required by Biometric's `BiometricPrompt`) | 1.9.0 |
| Wear Compose (Material 3, Foundation) | 1.6.2 |
| Play services Wearable | 20.0.1 |
| kotlinx-coroutines / kotlinx-serialization | 1.11.0 / 1.11.0 |

Libraries below Room in this table arrive with later changes and are listed so the baseline is visible in one place.

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
