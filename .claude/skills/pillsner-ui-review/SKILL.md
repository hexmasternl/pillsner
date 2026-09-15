---
name: pillsner-ui-review
description: Review Pillsner Compose UI code against docs/design-system.md and report violations - hard-coded colours or sizes, wrong type roles, red used outside danger, missing accessibility semantics, inline strings, dynamic colour, truncation, missing previews. Use before declaring any UI task done, when asked "does this follow the design system?", or when reviewing a change that touches ui/.
metadata:
  author: pillsner
  version: "1.0"
---

Audit UI code for compliance and report findings a developer can act on. This skill only finds and reports; fixing is the caller's decision unless they asked for fixes.

**Source of truth:** `docs/design-system.md`. Cite the section number with every finding.

## Scope

Default to the files changed in the working tree (`git -C src status --porcelain` and `git -C src diff --name-only`), restricted to `src/app/src/main/java/**/ui/**` and `res/values/strings.xml`. If the caller names files or a change folder, review those. If the whole UI is requested, walk every file under `ui/`.

## Automated sweep

Run each check from the repository root. A hit is a finding unless the exception column applies.

| # | Section | Command | Exception |
| --- | --- | --- | --- |
| 1 | 2.4 | `grep -rnE "Color\(0x|Color\.(Red|Green|Blue|White|Black|Gray|Yellow|Cyan|Magenta)\b" src/app/src/main/java --include=*.kt` | `ui/theme/Color.kt` |
| 2 | 1.5 | `grep -rn "dynamicLightColorScheme\|dynamicDarkColorScheme\|dynamicColor" src/app/src/main/java` | none |
| 3 | 3.3 | `grep -rnE "fontSize\s*=\s*[0-9]+(\.[0-9]+)?\.sp" src/app/src/main/java --include=*.kt` | `ui/theme/Type.kt` |
| 4 | 3.3 | `grep -rnE "\bTextStyle\(" src/app/src/main/java --include=*.kt` | `ui/theme/Type.kt`; `.copy(` on a theme style is fine |
| 5 | 5 | `grep -rnE "\b[0-9]+\.dp\b" src/app/src/main/java --include=*.kt` | `ui/theme/Dimens.kt`, `ui/theme/Shape.kt`; an icon size that matches a `Sizes` constant is a finding to replace with the constant |
| 6 | 4 | `grep -rnE "RoundedCornerShape\(" src/app/src/main/java --include=*.kt` | `ui/theme/Shape.kt` |
| 7 | 10 | `grep -rnE "maxLines\s*=\s*1|TextOverflow\.Ellipsis" src/app/src/main/java --include=*.kt` | inside a `TopAppBar` title |
| 8 | 8.2, 10 | `grep -rnE "\.alpha\(|alpha\s*=\s*0\.[0-9]" src/app/src/main/java --include=*.kt` | none: de-emphasis uses surface tiers and `onSurfaceVariant` |
| 9 | 3.3 | `grep -rnE "Text\(\s*\"[A-Za-z]" src/app/src/main/java --include=*.kt` | functions annotated `@Preview*` and files ending in `Preview.kt` |
| 10 | 3.3 | `grep -rnE "String\.format|SimpleDateFormat|\"HH:mm\"|\"hh:mm\"" src/app/src/main/java --include=*.kt` | none: use `DateFormat` or `DateTimeFormatter.ofLocalized*` |
| 11 | 10 | `grep -rnE "Icon\((?!.*contentDescription)" -P src/app/src/main/java --include=*.kt` | none; `contentDescription = null` beside a label is correct and passes |
| 12 | 2.4 | `grep -rnE "colorScheme\.(error|onError|errorContainer|onErrorContainer)" src/app/src/main/java --include=*.kt` | list every hit; each must be overdue, missed, undeliverable reminders, delete confirmation, empty stock or a validation error. Anything else is a finding. |
| 13 | 3.2 | `grep -rnE "typography\.(displayLarge)" src/app/src/main/java --include=*.kt` | at most one per screen composable |
| 14 | 10, 11 | `grep -rLE "@PreviewLightDark" $(grep -rlE "^@Composable" src/app/src/main/java/**/ui --include=*.kt)` | files in `ui/theme` that define no composable UI; view models |
| 15 | 7 | `grep -rn "res/layout\|setContentView\|LayoutInflater" src/app/src/main` | none: Compose only |
| 16 | 1.4 | `grep -rn "isSystemInDarkTheme" src/app/src/main/java/**/ui/theme/Theme.kt` | must be present exactly as the `darkTheme` default |

Commands 11 and 14 are approximations. Confirm each hit by reading the code before reporting it.

## Manual review

For each composable in scope, read it and answer these. Any "no" is a finding.

**Colour (2)**
- Is every colour a `colorScheme` role or `intakeStatusColors(...)`?
- Does the intake state mapping match 2.3 exactly (due blue, taken green, snoozed teal, skipped neutral, overdue and missed red)?
- Is red limited to the 2.4 list?

**Type (3)**
- Is the screen title `displayLarge`, exactly once, with `heading()` semantics?
- Is running text `bodyLarge` by default, with `bodyMedium` only for secondary lines and `bodySmall` only for timestamps and helper text?
- Are medicine names `titleMedium` and dose times `titleLarge` with tabular figures?
- Is Raleway (any display or headline role) never used for anything under 24 sp, and never for body copy?

**Layout (5, 6)**
- Is the screen edge `Spacing.screenEdge` and the gap between tiles `Spacing.lg`?
- Do tiles use `shapes.large`, the correct container tier for light and dark, and no border or shadow?
- Does a scrolling list under a FAB pad its bottom by the FAB height plus `Spacing.lg`?
- Does the content column cap at `Spacing.contentMaxWidth` on wide screens?

**Components (8)**
- Does each component match its recipe in 8.x: dose tile has a stripe, status chip has an icon, banner is `errorContainer` and only when reminders are undeliverable, FAB is the standard 56 dp size and `primaryContainer`, navigation has three labelled items?
- Is the reminder action order took it, not yet, not going to?
- Is the destructive button inside an `AlertDialog` only?

**Accessibility (10)**
- Are all touch targets at least 48 dp, the primary confirm 56 dp and in the bottom third?
- Is each tile one merged node with a full description; do chips carry `stateDescription`; do icon-only controls carry a `contentDescription`?
- Does anything truncate other than the app bar title?
- Is there a `@Preview(fontScale = 2f)` for every screen with a list or form, and does the layout wrap in it?

**Strings and copy**
- Are all user-facing strings in `strings.xml`, sentence case, verb-first on buttons, no exclamation marks, no scolding?
- Are plurals in `<plurals>`?

## Report format

Lead with the verdict: **Compliant**, or **N findings**. Then a table:

| File:line | Section | Finding | Fix |
| --- | --- | --- | --- |

Severity order: anything that breaks accessibility or uses red outside danger first, then hard-coded values, then type and spacing, then copy. Quote the offending line. Describe the fix in one sentence using the token name. Do not pad with things that passed; a short "Checked: colour, type, layout, components, accessibility, strings" line at the end is enough.

If `src/` has no code yet, say so and stop.
