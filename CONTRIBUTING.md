# Contributing to Pillsner

Thanks for your interest in Pillsner. This document covers everything you need to build, run and contribute to the project. For what Pillsner *is* — the product, its features and its privacy stance — see [`README.md`](README.md).

To keep the project coherent:

- Start non-trivial work with a change proposal rather than a surprise pull request.
- Branch from `development`, not `main`, and open pull requests against `development` — see [Branching model](#branching-model) below.
- Keep the app small and focused. A feature that does not directly help someone take their medication correctly probably belongs in a different app.
- Preserve the privacy stance. Do not introduce network access, third-party SDKs or telemetry without an accepted proposal that explains the trade-off.
- Write tests for scheduling and intake logic. Reminder timing is the one thing this app must never get wrong.
- Use clear, conventional commit messages that describe the intent of the change.

Bug reports and feature ideas can be filed as GitHub issues on this repository.

## Prerequisites

- Android Studio, latest stable release, with the Android SDK installed (SDK Platform 37 and Build Tools 36.0.0).
- JDK 21. Android Studio bundles it; on the command line, point `JAVA_HOME` at a JDK 21 installation.
- An Android device or emulator. Because the app schedules exact alarms and posts notifications, testing on a physical device gives the most realistic picture of reminder reliability.

## Toolchain

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
| WorkManager (the reminder watchdog) | 2.11.2 |
| Biometric | 1.1.0 |
| Fragment (host required by Biometric's `BiometricPrompt`) | 1.9.0 |
| Wear Compose (Material 3, Foundation) | 1.6.2 |
| Play services Wearable | 20.0.1 |
| kotlinx-coroutines / kotlinx-serialization | 1.11.0 / 1.11.0 |

Every library in this table is now in use.

## Building and running

1. Clone the repository.
2. Open the `src` folder in Android Studio and let Gradle sync finish.
3. Select a device or emulator and press Run. The project has two applications: `app` for the phone and `wear` for the watch. To try the pair, run `app` on a phone or emulator and `wear` on a paired Wear OS one.

The two applications share one application id and must be signed with the same certificate, or the Wearable Data Layer will not connect them. Debug builds do this on their own: both modules sign with the debug keystore Android Studio keeps in your `.android` folder. For a release build, sign both with the same key; no keystore or signing configuration is committed to this repository.

From a terminal inside the `src` folder, the usual Gradle wrapper tasks apply: `assembleDebug` produces a debug build, `test` runs the unit tests, and `connectedAndroidTest` runs the instrumented tests on an attached device.

## Permissions the app asks for

Pillsner needs permission to post notifications and to schedule exact alarms. Both are essential to its purpose. It asks for nothing else: no location, no contacts, no network, and — since it never opens the dialog — not the battery-optimisation exemption either. Reaching that setting is something you can do from the Home banner if a reminder ever fails to arrive; the app does not ask for it, and holds no permission to. The full list, with the reason for each, is in [Permissions](README.md#permissions) in the README.

## Development workflow

Pillsner uses a spec-driven workflow powered by [OpenSpec](https://github.com/Fission-AI/OpenSpec). Instead of jumping straight into code, every meaningful change starts as a proposal that captures **what** is changing and **why**, followed by a design that captures **how**, and a task list that breaks the work into steps.

The typical loop is:

1. **Propose** a change. A new folder appears under `openspec/changes/` with a proposal, a design and a task list.
2. **Apply** the change by working through the tasks and updating the code in `src`.
3. **Archive** the change once it is complete. The relevant specs in `openspec/specs/` are updated to reflect the new agreed behaviour, and the change moves to `openspec/changes/archive/`.

The `openspec/specs/` folder is therefore the living description of how Pillsner behaves. When the code and a spec disagree, either the code has a bug or the spec needs a change proposal. Never silently drift.

## Branching model

- **`main`** always mirrors what's running in production. `release.yml` builds, signs and publishes to Google Play on every push to it, so nothing commits there directly — it's only updated by a `development` → `main` pull request, opened manually when a release is due.
- **`development`** is the integration branch. Every finished feature lands here first, by pull request.
- **Feature branches** are cut from `development`, one per OpenSpec change (or trivial fix), named `feature/<change-name>`, and merge back into `development` by pull request once done.

Cutting a release is a deliberate, manual step: open a pull request from `development` into `main` when you want the accumulated features to ship. See `.claude/skills/git-workflow/SKILL.md` for the full lifecycle.
