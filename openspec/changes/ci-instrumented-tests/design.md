# Design

## Context

`ci.yml` has one job, `build`, on `ubuntu-latest`: unit tests, lint, debug assembly, reports uploaded on failure. It triggers on every pull request and on pushes to `main` and `development`, skipping `src/website/**`. The `app` module has about 430 instrumented tests across 56 files, `wear` 16 across 3. `wear`'s manifest declares `android.hardware.type.watch`. Both modules share `minSdk` 26 and `targetSdk`/`compileSdk` 37 from the version catalog. Locally the suite runs on a `Pixel_10` AVD with an API 37 Google APIs x86_64 image.

Why this change exists is in proposal.md. The behaviour it must have is in `specs/instrumented-test-gate/spec.md`.

## Goals / Non-Goals

**Goals:**
- One emulator definition, in the build, used both by CI and by anyone who wants a clean local full run.
- A full run that fits comfortably inside a CI job's time limit and gets faster on repeat runs.

**Non-Goals:**
- Selecting affected tests in CI. Selection is a local speed-up; CI is the backstop and runs everything.
- Running on physical devices or a device farm.
- Making the check required for merging. That's a ruleset setting the owner applies by hand (task 3.3).
- Changing any test. Tests that turn out to be flaky on CI are fixed in their own change, not hidden here.

## Decisions

### D1. Gradle Managed Devices, not a third-party emulator action

The emulator is defined in each module's `testOptions.managedDevices` and run with the generated `<device>DebugAndroidTest` task. The Android Gradle Plugin downloads the system image, creates the AVD, boots it headless, runs the tests, collects the reports and shuts it down.

Alternative considered: `reactivecircus/android-emulator-runner`, the common GitHub Action for this. It works, but it's a third-party action that would run with the repository's CI permissions, and `CLAUDE.md` prefers first-party tooling unless a third-party one removes meaningful complexity. Gradle Managed Devices removes the same complexity, is part of AGP (no new dependency), and keeps the device definition in the build where a local run can use it too.

### D2. One phone device and one watch device

- `app`: a phone device at API 37 (the `targetSdk`), using an x86_64 `google` or `aosp-atd` image, whichever exists for API 37. ATD (Automated Test Device) images boot faster and use less memory; use one if it exists for the chosen API level, otherwise the Google APIs image.
- `wear`: a watch device at the newest API level a Wear OS x86_64 image exists for. `wear` declares the watch feature, so a watch image is the faithful target.

**Chosen (task 1.1, checked against Google's system-image repository on 23 October 2026):**

- `app` → `phoneApi37`: Pixel 6, API 37, `google-atd`, x86_64 (`system-images;android-37.0;google_atd;x86_64`). All three candidates exist for API 37 (`aosp_atd`, `google_atd`, `google_apis`). ATD is chosen for boot time and memory; the Google variant rather than AOSP because the app's Wear sync goes through Play services' Wearable data layer, and the local reference AVD is a Google APIs image too, so tests see the same platform services in both places.
- `wear` → `watchApi37`: Wear OS Small Round, API 37, `android-wear-signed`, x86_64 (`system-images;android-37.0;android-wear-signed;x86_64`). API 37 is also the newest Wear OS x86_64 image, so the watch tests at the `targetSdk` as well. There is no Wear ATD image. AGP has no alias for Wear sources; it passes `android-wear-signed` through to the package path unchanged.

Testing at `minSdk` (26) as well is out of scope for this change. It would double the job time, and it's better proposed separately with evidence it's needed.

### D3. A separate CI job, parallel to `build`

A new job `instrumented-tests` in `ci.yml`, next to `build`, running `./gradlew :app:<phone>DebugAndroidTest :wear:<watch>DebugAndroidTest`. It runs only for `pull_request` events, not for the push that follows a merge: the same commit was already tested as a pull request, and pushes to `main` are releases that shouldn't wait on another emulator run. Path filters stay those of the workflow, so website-only pull requests skip it.

It needs KVM: on `ubuntu-latest` the job adds the standard udev rule that gives the runner user access to `/dev/kvm` before running Gradle. Without KVM the emulator falls back to software emulation and the run takes many times longer.

### D4. Cache the SDK images and the AVD snapshot

`actions/cache` keyed on the managed device definitions (a hash of both `build.gradle.kts` files' `managedDevices` blocks, or simply the two files) and the runner OS, covering `~/.android/avd` and the downloaded system images under the SDK. A cache hit skips the image download and lets the AVD resume from its snapshot rather than cold-boot.

### D5. Sharding only if the measured run needs it

The first implementation runs one device per module. If the measured job time is over 30 minutes, enable AGP's managed-device sharding (`android.experimental.androidTest.numManagedDeviceShards`) for `app`, sized to the runner's cores. This doesn't change the spec, only how fast it's met.

### D6. Failure reports

On failure, the job uploads `src/*/build/reports/androidTests/**` and `src/*/build/outputs/androidTest-results/**`, the same way the `build` job uploads its reports.

## Risks / Trade-offs

- [No x86_64 API 37 image available to managed devices yet] → Use the newest API level that has one, record it in the device block's comment, and raise the level when the image appears. Task 1.1 checks this before anything else is built.
- [Tests that pass locally are flaky on a slower CI emulator] → Animations are disabled by managed devices by default. A test that is still flaky gets its own fix, as an issue. It is never retried silently or excluded in this change.
- [Job time] → Caching (D4), an ATD image where possible (D2), sharding if needed (D5). The job runs in parallel with `build`, so pull request feedback is delayed only by the difference between the two.
- [GitHub-hosted runner minutes] → The repository is public, so standard runner minutes are free. Pushes after merge don't re-run the job (D3).
- [The first CI run of the watch suite failed 14 of 16 tests] → `wear` didn't declare `espresso-core`, so it resolved 3.5.0 through `ui-test-junit4`, which looks up the hidden `InputManager.getInstance` by reflection and fails on API 37. `wear` now declares the catalog's `espresso-core`, as `app` already did. No new library; the tests themselves are unchanged. The local `Pixel_10` runs never caught it because they only run `app`. With that fixed, 5 of the 16 watch tests still fail on the watch: four UI tests assert on content below the fold of the small round screen (#104), and the data-layer update test times out (#105). Per the risk below they are fixed in their own changes, not here, so the `instrumented-tests` check stays red until those land.
- [Both setup tasks downloading their system image at once] → the install failed with "Archive is not a ZIP archive". The job installs both images with `sdkmanager` before Gradle runs; the device blocks and that step must name the same images.
- [Wear tests need a watch image that may be large or slow] → If the watch device can't run reliably on the runner, fall back to running `wear`'s tests on the phone device and record why in this design. The spec only requires that they run on an emulator.

## Migration Plan

The new job is advisory until the owner adds it as a required status check to the `protect-development` ruleset (task 3.3). Rolling back means removing the job and the two `managedDevices` blocks; nothing else depends on them.

## Open Questions

- The exact API level and image source of each device depend on which images exist at implementation time (task 1.1). This changes a value in the device blocks, not the approach.
