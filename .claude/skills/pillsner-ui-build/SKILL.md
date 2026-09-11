---
name: pillsner-ui-build
description: Build a Pillsner screen or composable that complies with docs/design-system.md - dose and medicine tiles, status chips, buttons, FAB, bottom navigation, attention banner, empty states, forms, dialogs, sheets and the reminder notification. Use whenever creating or changing anything in ui/ that a user sees. Gives the recipe, the token to use for every decision, the accessibility contract and the previews each composable must ship with.
metadata:
  author: pillsner
  version: "1.0"
---

Build UI that a reviewer can pass against `docs/design-system.md` without discussion.

**Read first:** `docs/design-system.md` sections 5 (spacing), 8 (components), 9 (motion), 10 (accessibility). Then the proposal and design of the change you are working in, under `openspec/changes/<change>/`.

**Depends on:** the theme layer from the `pillsner-theme` skill. If `ui/theme` is missing or incomplete, run that skill first.

## Workflow

1. **Name the surface.** Which screen, which component, which state does it show? Find it in section 8. If it is not there, it is either a composition of things that are, or it needs a design-system addition: say which, and do not improvise.
2. **Sketch the states before the layout.** Every list has an empty state (8.10). Every form field has an error state (8.11). Every dose has one of six intake statuses (2.3). Write the `UiState` so those states are explicit.
3. **Build from Material 3 components** using the recipes in `reference/components.md`. Do not write a custom widget where a Material component exists.
4. **Wire tokens, not values.** Use the lookup table below for every decision.
5. **Add semantics** per the accessibility contract below.
6. **Add previews**: `@PreviewLightDark` on every composable, plus `@Preview(fontScale = 2f)` on every screen with a list or a form.
7. **Add strings** to `res/values/strings.xml` with a `pillsner_` free, descriptive name (`home_title`, `dose_status_taken`, `reminder_action_took_it`). Plurals go in `<plurals>`. Times and dates are formatted through `DateFormat` or `DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)`.
8. **Write the Compose test** the change's tasks ask for (semantics-based: the screen shows N tiles, the empty state appears, the FAB navigates).
9. **Run `pillsner-ui-review`** on your files before reporting.

## Decision to token lookup

| You need | Use |
| --- | --- |
| A colour | `MaterialTheme.colorScheme.<role>`; for a dose status `intakeStatusColors(status)` |
| A text style | `MaterialTheme.typography.<role>` from section 3.2. Default `bodyLarge`. Screen title `displayLarge`, once. Medicine name `titleMedium`. Dose time `titleLarge` with `fontFeatureSettings = "tnum"`. Button `labelLarge` (Material default). |
| A corner | `MaterialTheme.shapes.large` for tiles and dialogs, `extraLarge` for sheets and the large FAB, `small` for chips; buttons keep their Material pill |
| Space between things | `Spacing.xs/sm/md/lg/xl/xxl/xxxl`. Screen edge `Spacing.screenEdge`. Between tiles `Spacing.lg`. Below a screen title `Spacing.xl`. |
| A size | `Sizes.minTouchTarget`, `Sizes.primaryActionHeight`, `Sizes.tileMinHeight`, `Sizes.stateStripeWidth`, `Sizes.statusChipHeight`, `Sizes.iconDefault`, `Sizes.iconEmptyState` |
| A tile container | Light: `surfaceContainerLowest`; dark: `surfaceContainerHigh`. Use `CardDefaults.cardColors(containerColor = tileContainerColor())` from the reference. |
| An inactive tile | `surfaceContainerLow` container, all text `onSurfaceVariant`, `Inactive` chip, `stateDescription`. Never alpha. |
| An icon | `Icons.Outlined.*` at rest, `Icons.Filled.*` when selected or active, `Sizes.iconDefault` |
| Something red | Only: overdue, missed, undeliverable reminders, delete confirmation, empty stock, validation error. Anything else is not red. |
| A duration | 150 ms short, 250 ms medium, 350 ms long; `EmphasizedDecelerate` in, `EmphasizedAccelerate` out. No bounce. |

## Accessibility contract

Every composable you ship satisfies all of these:

- Every tappable thing is at least `Sizes.minTouchTarget` square. The primary confirm is `Sizes.primaryActionHeight` tall, full width on compact screens, in the bottom third.
- A tile is one node: `Modifier.semantics(mergeDescendants = true) { contentDescription = ... }` with a full sentence built from string resources, e.g. "Ibuprofen, 40 milligrams, due at 08:00". Status chips inside carry `stateDescription`.
- Icon-only controls have a `contentDescription`; icons next to a label that says the same thing pass `null`.
- Headings use `Modifier.semantics { heading() }`.
- No `maxLines = 1` or `TextOverflow.Ellipsis` except on a `TopAppBar` title. Text wraps.
- Colour never carries meaning alone: status has icon plus label plus colour.
- Nothing reads system font scale or overrides it. Every size is `sp` for text and `dp` for layout.
- Animations respect the system animator scale (Compose does this by default; do not use `withFrameNanos` timers that bypass it).

## Copy

Sentence case. Verb first on buttons ("Save", "I took it", "Not yet", "Not going to", "Open settings"). No exclamation marks. Error messages say what went wrong and how to fix it: "Must be on or after the 'used since' date, 11 Sep 2026." Empty states are friendly and specific: "Nothing due right now" / "Your next dose will appear here." Never scold: a skipped dose is "Skipped", not "Missed" and not "You skipped this".

## Definition of done

- Compiles, lint clean, unit and Compose tests for the change pass locally (run from `src/`). Failures reported verbatim.
- `pillsner-ui-review` passes with no findings, or every remaining finding is listed with a reason.
- Both previews render in Android Studio without red text or clipped content.
- Strings are in resources; no user-facing literal in a non-preview composable.
