---
name: emulator-tests
description: Run Pillsner's instrumented (androidTest) tests on the emulator quickly - reuse a running emulator or boot one headless, disable animations, work out which test classes a change affects from the git diff, and run only those, leaving the full suite to CI's instrumented-tests job on every pull request (a full local run only on request). Used by the pillsner-test-runner agent for the emulator mechanics and the selection. Use whenever instrumented tests need to run - during /opsx:apply after a task touches app or wear code, before a change's pull request is opened, when asked to "run the emulator tests" or "run the connected tests", and when a single failing instrumented test needs re-running.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

The instrumented suite (about 430 tests in `app`, 19 in `wear`) is the slow part of finishing a
change. Most of a change's risk sits in a handful of packages, so most of the suite doesn't need
to run for it. This skill runs the part that does, and says plainly what it skipped.

CI runs the full instrumented suite of both modules on every pull request that touches anything
outside `src/website/` (the `instrumented-tests` job in `.github/workflows/ci.yml`, on Gradle
Managed Devices; a website-only pull request doesn't start `ci.yml`), so a selective run here is the fast
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
   where `<avd>` comes from `<sdk>/emulator/emulator -list-avds`: the phone AVD for `app`
   (on this machine: `Pixel_10_Pro`) and `Wear_Small_Round_API_36` for `wear` (see "The watch
   emulator" below). Both can run at once; pick the device per module with `ANDROID_SERIAL`.
   Leave the default quick-boot snapshot on — it turns a cold boot into a resume. Then wait for it:
   `adb wait-for-device shell 'while [ -z "$(getprop sys.boot_completed)" ]; do sleep 1; done'`.
   Never create or delete an AVD unasked, except `Wear_Small_Round_API_36` as described below; if
   there is no phone AVD, stop and tell the user.
4. Turn animations off (cheap, and it removes a common source of slow and flaky Compose tests):

   ```bash
   adb shell settings put global window_animation_scale 0
   adb shell settings put global transition_animation_scale 0
   adb shell settings put global animator_duration_scale 0
   ```

5. Leave the emulator running when done. The next task in this change, or the next change, reuses
   it. Shut it down only when the user asks.

### The watch emulator

`wear` tests run on `Wear_Small_Round_API_36`: a `wearos_small_round` AVD (384×384, round) on
`system-images;android-36;android-wear-signed;x86_64` (Wear OS 6). Not the phone emulator, because
`RoundScreenFitTest` only means something on a round screen and skips itself elsewhere. Not a Wear
OS API 37 image locally: it's the one CI uses (`watchApi37`, see "A clean full run locally"), and the
API 36 AVD keeps a second Wear OS version covered. If every Compose UI test crashes with
`NoSuchMethodException: android.hardware.input.InputManager.getInstance` on API 37, the module
resolved Espresso 3.5.0 through `ui-test-junit4`; `wear` declares the catalog's `espresso-core` so
it gets 3.7.0, which works there.

If the AVD is missing, create it (this is the one AVD the skill may create unasked). Windows needs
`cmd /c` and the full path, because the `.bat` wrappers split a package path at `;`:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cmd /c 'echo y| D:\Android\SDK\cmdline-tools\latest\bin\sdkmanager.bat --sdk_root=D:\Android\SDK "system-images;android-36;android-wear-signed;x86_64"'
cmd /c 'echo no| D:\Android\SDK\cmdline-tools\latest\bin\avdmanager.bat create avd -n Wear_Small_Round_API_36 -k "system-images;android-36;android-wear-signed;x86_64" -d wearos_small_round'
```

Then set `hw.lcd.circular=true` in `<avd home>/Wear_Small_Round_API_36.avd/config.ini`;
`avdmanager` writes `false`, which makes the screen square. Check it after boot:
`adb shell wm size` says `384x384`, and `adb shell dumpsys window | grep RoundedCorner` shows a
radius of 192.

On this small screen most of a watch list starts below the fold, so watch UI tests scroll the column
to an item (`performScrollToNode`) before `assertIsDisplayed()`, and count items through the
column's `CollectionInfo` (`itemCount()` in `ColumnAssertions.kt`), not by counting composed nodes
(#104).

Only one connected test run at a time per emulator. Two worktrees running
`connected*AndroidTest` against the same device uninstall each other's APKs mid-run.

## 2. Work out what to run

Compare against where the change started: `git diff --name-only development...HEAD`, plus
uncommitted files (`git status --porcelain`). During `/opsx:apply`, a run after a single task may
narrow this to that task's files; the run before the pull request uses the whole branch diff.

**Wide blast radius** — these files affect almost every test, but the full suite does not run
locally for them; CI's `instrumented-tests` job runs it on the pull request. Select for the rest of
the diff as below, and name these files in the report as covered only by CI:

- Any `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` or `gradle.properties`
- An `AndroidManifest.xml`
- `MainActivity`, `AppContainer`, the application class, `ui/theme/`, `ui/navigation/`
- Shared test infrastructure under `androidTest` that isn't itself a test class (fixtures,
  rules, runners)

If such files are the only thing changed, run no instrumented tests and say so.

**Build the class list** from these rules, de-duplicated. Only tests the change affects run; there
is no fixed safety set, because CI's full run on every pull request is the backstop:

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

- `wear` the same way with `:wear:connectedDebugAndroidTest`, with `ANDROID_SERIAL` set to the
  watch emulator. If `Wear_Small_Round_API_36` can't be created or booted, report the wear tests as
  **not run**, and never run them on the phone emulator instead; CI still runs them on the build's
  own watch device (see "A clean full run locally").
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

- Mode: **selective**, or **full** when the user asked for it; and any wide-blast-radius files left to CI.
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
./gradlew :app:phoneApi37DebugAndroidTest -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
./gradlew :wear:watchApi37DebugAndroidTest -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
```

This is also how to run the `wear` tests on a real Wear OS image. Results land under
`<module>/build/reports/androidTests/managedDevice/` and
`<module>/build/outputs/androidTest-results/managedDevice/`. They are much slower than a selective
run on the already-running emulator, so use them when CI reports a failure you can't reproduce, or
when the user asks for a full run that matches CI.

## Guardrails

- Never mark a failing test `@Ignore`, delete it, or narrow the selection to drop it. Fix it, or
  report it as failing.
- If the user asks for the full suite, run the full suite.
- Never let a selective local run stand in for the CI run: a pull request whose
  `instrumented-tests` check failed is not ready, whatever the local selection said.
- Never change Gradle configuration, add a test runner argument to `build.gradle.kts`, or add or
  change a Gradle Managed Device from this skill — those change the build and need an OpenSpec
  change.
