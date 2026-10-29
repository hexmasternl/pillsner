# Proposal

## Why

**GitHub Issue:** #93 (https://github.com/hexmasternl/pillsner/issues/93)

No instrumented (androidTest) test runs in CI: `ci.yml` runs unit tests, lint and a debug assembly, and the roughly 446 instrumented tests in `app` and `wear` run only when someone runs them on a local emulator. That makes the local emulator run both slow (the whole suite runs for every change) and the only gate the scheduling, notification, database and UI-flow tests have. Running the full suite in CI on every pull request into `development` lets local runs stay selective (the `emulator-tests` skill) while every merge is still gated by the whole suite.

## What Changes

- Add Gradle Managed Devices to the `app` and `wear` modules: a phone device for `app` and a watch device for `wear`, so the same emulator definition runs in CI and, on demand, locally.
- Add an instrumented-test job to `.github/workflows/ci.yml` that runs the full instrumented suite of both modules on those managed devices for every pull request into `development` and `main`, in parallel with the existing build job.
- The job fails when any instrumented test fails and uploads the test reports when it does.
- Cache the downloaded system images and the device snapshot between runs so the job doesn't download and cold-boot every time.
- Update the `emulator-tests` skill so its report says the full suite runs in CI, and update the README's description of `ci.yml`.

## Capabilities

### New Capabilities
- `instrumented-test-gate`: Continuous integration runs the full instrumented test suite of every Android module on an emulator for each pull request, and a failing instrumented test fails the pull request's checks.

### Modified Capabilities
<!-- None: no app behaviour changes. -->

## Impact

- `src/app/build.gradle.kts`, `src/wear/build.gradle.kts`: a `testOptions.managedDevices` block each. No new dependency; Gradle Managed Devices is part of the Android Gradle Plugin.
- `.github/workflows/ci.yml`: a new job. It uses only actions the workflow already uses (`actions/checkout`, `actions/setup-java`, `gradle/actions`, `android-actions/setup-android`, `actions/upload-artifact`) plus `actions/cache`.
- CI time: the new job is the longest job on a pull request. It runs in parallel with the build job, so the wall-clock time of a pull request's checks grows only by the difference between the two.
- The app itself does not change: no permission, dependency, network access or behaviour change, and nothing in the shipped APKs or bundles.
- Making the new check required for merging into `development` is a GitHub ruleset setting the repository owner changes by hand. This change documents the setting but does not apply it.
