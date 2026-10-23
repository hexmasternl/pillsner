---
name: verify-change
description: Run exactly the checks a Pillsner change needs before its pull request, cheapest first - unit tests, lint, assembly, UI review, selective emulator tests, device scenarios, Room and website checks - chosen from the branch's diff, and write the result as the Verification section of tasks.md that the pull request summary is built from. Use when a change's last task is done and before github-openspec-sync apply-complete runs, when asked "is this ready?" or "verify the change", and after fixing a failure found by CI or review.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

One gate, run once per change before its pull request, instead of a set of checks spread over
`CLAUDE.md` and several skills. It decides what to run from the diff, runs the fast checks first so a
lint error doesn't cost an emulator run, and writes down what ran and what didn't.

Run it from the change's worktree (`../pillsner-worktrees/<name>`). Gradle commands run from `src/`.

## 1. Classify the diff

`git diff --name-only development...HEAD` plus uncommitted files. Note which of these the change
touches; each one switches on a step below.

| Touched | Switches on |
| --- | --- |
| Anything under `src/app`, `src/wear`, `src/shared` | Steps 2–3 (always for app code) |
| `ui/` composables, theme, `res/` | Step 4 |
| Main code with instrumented tests (`data/`, `ui/`, `applock/`, wear) | Step 5 |
| `data/reminders/`, the manifest's receivers, alarm or notification code | Step 6 |
| `data/db/`, an `@Entity`, a DAO, `app/schemas/` | Step 7 |
| `src/shared/` | Step 8 |
| `src/website/` | Step 9 |
| Only `openspec/`, `docs/`, `.claude/`, `.github/` | Step 9 if website, otherwise nothing to build — say so |

## 2. Fast checks (stop at the first failure)

```bash
./gradlew test          # unit tests, every module
./gradlew lint          # includes MissingTranslation/ExtraTranslation as errors (app/lint.xml)
./gradlew assembleDebug # also runs the build permission guard
```

These are what CI's `build` job runs; passing them locally means CI's build job will pass. Fix a
failure before going further — every later step costs more.

## 3. Strings

If any `res/values*/strings.xml` changed, lint above already proves every locale has every key. Also
check the `app-strings` skill's review list (placeholders, plurals, glossary terms) for the keys that
changed.

## 4. UI review

Run the `pillsner-ui-review` skill over the changed composables. Its findings are fixed, not listed.

## 5. Instrumented tests

Run the `emulator-tests` skill (selective, with its safety set). Keep its report — the "N of M"
line goes into the verification section.

## 6. Device scenarios

When reminder delivery, alarms, notifications or the boot/time receivers changed, run the matching
scenarios from the `adb-device-scenarios` skill: at least reboot, and the time zone, DST and
clock-change scenarios when scheduling code changed. Each scenario that passes turns a "manual test"
into a verified one.

## 7. Database

Walk the `room-schema-change` skill's checklist: version bumped, migration in `Migrations.ALL`,
schema JSON exported and committed, migration test added and passing.

## 8. Wear contract

Walk the `wear-sync-contract` skill's compatibility checklist.

## 9. Website

From `src/website/`: `hugo --minify --panicOnWarning`, then delete `public/`, then the
`website-feature-sync` skill's structure check. If the change is user-visible and the website wasn't
touched, that's a missing website task — go back and do it.

## 10. Write the result

Add or replace a `## Verification` section at the end of the change's `tasks.md`:

```markdown
## Verification

- Unit tests: pass (`./gradlew test`)
- Lint: pass
- Assemble debug: pass
- UI review: 2 findings fixed (hard-coded padding in DoseTile, missing contentDescription)
- Instrumented tests: pass — selective, 12 of 59 classes (84 of 446 tests)
- Device scenarios: reboot pass, time zone move pass, DST end pass
- Not run: Room checks (no schema change), website (no user-visible change)
- Manual testing still needed: <list, or "none">
```

Every line says what ran and its outcome. Anything not run says so and why. A failure is copied
verbatim. This section is what `github-openspec-sync` apply-complete turns into the pull request's
verification status and manual-testing callout. Commit it with the change.

## Guardrails

- Never mark a step passed that didn't run. "Not run" with a reason is fine; a silent omission isn't.
- Never weaken a check to make it pass: no lint baseline additions, no `@Ignore`, no narrowed test
  selection, no `--continue` to step past a failure.
- A failure in a check unrelated to this change (a flaky test, something broken on `development`) is
  reported as such, with the evidence, not fixed inside this change.
