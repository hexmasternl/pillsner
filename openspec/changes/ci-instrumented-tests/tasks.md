# Tasks

## 1. Managed devices

- [x] 1.1 Find which x86_64 system images exist for API 37 (`aosp-atd`, `google-atd`, `google`) and for Wear OS (`sdkmanager --list`), pick each device's API level and image source per design D2, and record the choice and the reason in design.md D2
- [ ] 1.2 Add a phone managed device to `src/app/build.gradle.kts` `testOptions.managedDevices`, and verify `./gradlew :app:<phone>DebugAndroidTest` runs the full `app` suite locally and passes
- [ ] 1.3 Add a watch managed device to `src/wear/build.gradle.kts`, and verify `./gradlew :wear:<watch>DebugAndroidTest` runs the full `wear` suite locally and passes (or apply the fallback in design.md Risks and record it there)
- [ ] 1.4 Verify `./gradlew test lint assembleDebug` still passes and that the release bundle is unchanged by the device blocks (no new permission or dependency; the build permission guard passes)

## 2. CI job

- [ ] 2.1 Add the `instrumented-tests` job to `.github/workflows/ci.yml` per design D3 (pull requests only, KVM enabled, JDK and SDK set up as in `build`, both managed-device tasks run), and verify it passes on this change's own pull request
- [ ] 2.2 Add the cache from design D4, and verify a second run on the pull request reports a cache hit and is faster than the first
- [ ] 2.3 Add the failure report upload from design D6, and verify it by pushing a temporary deliberately failing instrumented test to the pull request, confirming the check fails and the reports artifact appears, then removing that test
- [ ] 2.4 Record the measured job duration in design.md D5, and enable sharding only if it is over 30 minutes; verify the sharded run still runs every test

## 3. Documentation and handover

- [ ] 3.1 Update the README's `.github/workflows/` row to describe the `instrumented-tests` job, and verify the description matches `ci.yml`
- [ ] 3.2 Update `.claude/skills/emulator-tests/SKILL.md` so its report and guardrails say the full suite runs in CI on every pull request, and document the managed-device tasks as the way to run a clean full suite locally
- [ ] 3.3 Manual: the repository owner adds the `instrumented-tests` check as a required status check to the `protect-development` ruleset on GitHub. Mention it prominently in the pull request summary
