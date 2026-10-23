---
name: emulator-tests
description: Run Pillsner's instrumented (androidTest) tests on the emulator quickly - reuse a running emulator or boot one headless, disable animations, work out which test classes a change affects from the git diff, and run only those plus a fixed safety set, falling back to the full suite only when the change has a wide blast radius. Use whenever instrumented tests need to run - during /opsx:apply after a task touches app or wear code, before a change's pull request is opened, when asked to "run the emulator tests" or "run the connected tests", and when a single failing instrumented test needs re-running.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

The instrumented suite (about 430 tests in `app`, 16 in `wear`) is the slow part of finishing a
change. Most of a change's risk sits in a handful of packages, so most of the suite doesn't need
to run for it. This skill runs the part that does, and says plainly what it skipped.

CI runs the full instrumented suite of both modules on every pull request (the `instrumented-tests`
job in `.github/workflows/ci.yml`, on Gradle Managed Devices), so a selective run here is the fast
local check and CI is the backstop that catches what selection missed. The skipped count still
goes into the report and the pull request summary, so a reviewer knows the full run's result is
the one to wait for.

All Gradle commands run from the `src` folder of the checkout you're working in — for a change
being applied, that's `../pillsner-worktrees/<name>/src`, not the main checkout.

## 1. Get an emulator ready

Booting is often the most expensive step, so never boot one that is already running.

1. Find the SDK from `src/local.properties` (`sdk.dir`, with the `\:` escape undone, e.g.
   `D:/Android/SDK`). `adb` is `<sdk>/platform-tools/adb`, the emulator is
   `<sdk>/emulator/emulator`.
2. `adb devices`. If an `emulator-*` entry is in state `device`, use it and skip to step 4.
3. Otherwise boot one headless, with the `run_in_background` option so it keeps running after this
   step:
   `<sdk>/emulator/emulator -avd <avd> -no-window -no-audio -no-boot-anim`
   where `<avd>` comes from `<sdk>/emulator/emulator -list-avds` (on this machine: `Pixel_10`).
   Leave the default quick-boot snapshot on — it turns a cold boot into a resume. Then wait for it:
   `adb wait-for-device shell 'while [ -z "$(getprop sys.boot_completed)" ]; do sleep 1; done'`.
   Never create or delete an AVD unasked; if there is none, stop and tell the user.
4. Turn animations off (cheap, and it removes a common source of slow and flaky Compose tests):

   ```bash
   adb shell settings put global window_animation_scale 0
   adb shell settings put global transition_animation_scale 0
   adb shell settings put global animator_duration_scale 0
   ```

5. Leave the emulator running when done. The next task in this change, or the next change, reuses
   it. Shut it down only when the user asks.

Only one connected test run at a time per emulator. Two worktrees running
`connected*AndroidTest` against the same device uninstall each other's APKs mid-run.

## 2. Work out what to run

Compare against where the change started: `git diff --name-only development...HEAD`, plus
uncommitted files (`git status --porcelain`). During `/opsx:apply`, a run after a single task may
narrow this to that task's files; the run before the pull request uses the whole branch diff.

**Full suite instead** — skip selection and run everything (step 3, no class filter) when the diff
touches any of these, because almost every test depends on them:

- Any `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` or `gradle.properties`
- An `AndroidManifest.xml`
- `MainActivity`, `AppContainer`, the application class, `ui/theme/`, `ui/navigation/`
- Shared test infrastructure under `androidTest` that isn't itself a test class (fixtures,
  rules, runners)
- More than roughly a third of the app's packages at once — selection buys little then

**Otherwise, build the class list** from these rules, de-duplicated:

1. **Same package.** For every changed main source file under
   `app/src/main/java/nl/hexmaster/pillsner/<pkg>/`, every test class in
   `app/src/androidTest/java/nl/hexmaster/pillsner/<pkg>/` (same package, not sub-packages). Same
   for `wear` (`wear/src/main/kotlin/...` → `wear/src/androidTest/kotlin/...`).
2. **Changed tests.** Every changed or added file under `androidTest` that contains `@Test`.
3. **Importers.** For every changed main class `X` in package `<pkg>`, every androidTest file that
   imports it: `grep -rl "import nl.hexmaster.pillsner.<pkg>.X$"` over both `androidTest` trees.
   `domain/` has no instrumented tests of its own, so this rule is how a domain change reaches the
   data and UI tests that exercise it. Go one level further only for `domain/` changes: also take
   the tests of the classes that import `X` in main code.
4. **Strings and resources.** For a changed `res/values*/strings.xml`, find the changed string ids,
   find the main files that reference `R.string.<id>`, and apply rule 1 to them. A changed
   drawable, font or layout resource → the same, via `R.drawable.<id>` and so on.
5. **`shared/` changes** — run all `wear` tests, plus the tests in `app/.../data/wear/` if any.
6. **Safety set**, always added because these are the parts `CLAUDE.md` calls fragile:
   - Anything under `data/` or `domain/` changed → every test in `data/reminders/` and
     `data/db/PillsnerDatabaseMigrationTest`.
   - A Room entity, DAO, database class or `app/schemas/` changed → every test in `data/db/`.
   - Anything under `ui/` changed → `ui/PillsnerAppNavigationTest`, `ui/DeadDestinationTest`,
     `ui/LegalGateNavigationTest`.

If the list comes out empty for a module (a docs-only change, or a change in `app` only, for
`wear`), say so and run nothing for that module.

Run a module's tests only when its list is non-empty: `wear` usually isn't touched, and skipping
`:wear:connectedDebugAndroidTest` also skips building and installing its APKs.

## 3. Run them

Pass the classes as a comma-separated list of fully-qualified names:

```bash
./gradlew :app:connectedDebugAndroidTest \
  "-Pandroid.testInstrumentationRunnerArguments.class=nl.hexmaster.pillsner.data.db.DoseDaoTest,nl.hexmaster.pillsner.ui.dose.DoseDetailScreenTest"
```

- `wear` the same way with `:wear:connectedDebugAndroidTest`. If its APK won't install on the
  phone emulator (a watch-only feature requirement), report the wear tests as **not run** — don't
  create a Wear AVD unasked; `:wear:watchApi37DebugAndroidTest` (below) runs them on the build's
  own watch device, and CI runs them on every pull request.
- Re-running one failing test while fixing it: `class=<fqcn>#<method>`. Re-run the full selection
  once the fix is in, not just the one test.
- Run Gradle with `run_in_background` for anything but a handful of classes and wait for the
  completion notification instead of polling.
- Results: `app/build/reports/androidTests/connected/` (HTML) and
  `app/build/outputs/androidTest-results/connected/` (XML). Report failures verbatim, as
  `CLAUDE.md` requires — the test name and the assertion message, not a paraphrase.

## 4. Report

Every run ends with a short report, and the last one before the pull request goes into the PR
summary (`github-openspec-sync`'s **apply-complete** mode) under its verification status:

- Mode: **selective** or **full**, and why full if it was full.
- How many test classes and tests ran, out of how many: e.g. "12 of 59 classes (84 of 446 tests)".
- Which rule pulled each class in, briefly — enough for a reviewer to see the selection was sound.
- Pass/fail, with failures verbatim.
- Timings: emulator ready (reused or booted, and how long), and the Gradle run. These show where
  the time actually goes, so the next speed-up can be aimed at the right step.

A selective run is never described as "all instrumented tests pass". Say "the affected
instrumented tests pass (selective run, N of M); the full suite runs in CI on the pull request".

## A clean full run locally

To run the full suite exactly as CI does — on a fresh emulator the build defines, without touching
the `Pixel_10` AVD — use the managed-device tasks (each one downloads its image on first use, boots
it headless, runs the module's whole suite and shuts it down):

```bash
./gradlew :app:phoneApi37DebugAndroidTest
./gradlew :wear:watchApi37DebugAndroidTest
```

This is also how to run the `wear` tests on a real Wear OS image. Results land under
`<module>/build/reports/androidTests/managedDevice/` and
`<module>/build/outputs/androidTest-results/managedDevice/`. They are much slower than a selective
run on the already-running emulator, so use them when CI reports a failure you can't reproduce, or
when the user asks for a full run that matches CI.

## Guardrails

- Never skip the safety set to save time.
- Never mark a failing test `@Ignore`, delete it, or narrow the selection to drop it. Fix it, or
  report it as failing.
- If the user asks for the full suite, run the full suite.
- Never let a selective local run stand in for the CI run: a pull request whose
  `instrumented-tests` check failed is not ready, whatever the local selection said.
- Never change Gradle configuration, add a test runner argument to `build.gradle.kts`, or add or
  change a Gradle Managed Device from this skill — those change the build and need an OpenSpec
  change.
