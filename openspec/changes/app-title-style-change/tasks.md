## 1. Preconditions

- [x] 1.1 Verify the scaffold exists: `src/` is a Gradle project, `ui/theme/PillsnerTheme` and `ui/home/WelcomeHeader.kt` are present. Stop and report if any of it is missing; this change does not create scaffold.
- [x] 1.2 Confirm `R.string.app_title` is still `Pillsner` and `translatable="false"`, and that `WelcomeHeader.kt` is the only caller of it.

## 2. The wordmark composable

- [x] 2.1 Create package `nl.hexmaster.pillsner.ui.components`.
- [x] 2.2 Add `PillsnerWordmark.kt` with an internal pure function that splits a title into a leading fragment and an accent fragment at the last occurrence of the private constant `"ner"`, returning the whole string as the lead and an empty accent when the fragment is absent or would leave an empty lead.
- [x] 2.3 Add the `PillsnerWordmark` composable: parameters `modifier: Modifier = Modifier` and `style: TextStyle = MaterialTheme.typography.displayLarge`; builds one `AnnotatedString` with `SpanStyle(color = MaterialTheme.colorScheme.secondary)` over the lead and `SpanStyle(color = MaterialTheme.colorScheme.primary)` over the accent; renders a single `Text`. No hex values, no hard-coded sizes, no second weight.
- [x] 2.4 Write KDoc stating the two-colour rule, why it exists (the Pills + Partner blend), that the colouring is decorative, and the design-system rule that Raleway is never used below 24 sp so `style` must stay at a display or large headline role.
- [x] 2.5 Add a `@PreviewLightDark` preview of the wordmark on a `Surface` inside `PillsnerTheme`.

## 3. Wire it into the welcome header

- [x] 3.1 Replace the title `Text` in `WelcomeHeader.kt` with `PillsnerWordmark`, keeping `TextAlign.Center`, the `semantics { heading() }` modifier and the surrounding layout and spacing exactly as they are.
- [x] 3.2 Remove the now-unused imports from `WelcomeHeader.kt`; leave `WelcomeScreenTestTags.HEADER` on the column untouched.
- [x] 3.3 Check the existing `WelcomeHeaderPreview` still renders in both schemes.

## 4. Tests

- [ ] 4.1 Unit test the split function: "Pillsner" splits into "Pills" / "ner"; a title without "ner" yields the whole string as the lead and an empty accent; a title that is exactly "ner" yields a non-empty lead rather than an empty one.
- [ ] 4.2 Extend the Compose test for the welcome screen to assert the header exposes exactly one node whose text is "Pillsner".
- [ ] 4.3 Run the unit test task from `src/` and report the result verbatim.
- [ ] 4.4 Run lint from `src/` and report the result verbatim. Instrumented tests are only needed for task 4.2's assertion; run them if the environment allows, and say so plainly if it does not.

## 5. Design system

- [ ] 5.1 Add a short "Wordmark" subsection to `docs/design-system.md` under section 2 or 8: the split, the two colour roles, the single-node rule, the minimum type role, and where the composable lives.
- [ ] 5.2 Mirror the same entry in `docs/design-system.html` so the two stay in step.

## 6. Review

- [ ] 6.1 Run the `pillsner-ui-review` skill over the new and changed files and fix anything it reports.
- [ ] 6.2 Confirm every scenario in `specs/welcome-screen/spec.md` is covered by a test or a documented visual check, and list which covers which.
