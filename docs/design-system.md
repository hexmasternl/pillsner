# Pillsner design system

> **Status: confirmed on 11 September 2026.** This document is the source of truth for every visual and interaction decision in the Pillsner Android app. The design agent and the UI skills in `.claude/` refer back to it; code in `src/` implements it.

Pillsner reminds people to take their medication and records whether they did. The interface has one job: make the next dose obvious and confirmable in one tap, at any hour, with any eyesight, in either hand. Everything below serves that.

---

## 1. Principles

1. **Calm by default, loud only when it matters.** Greens and blues carry the whole app. Red appears only when something is wrong or needs attention: a missed or overdue dose, reminders that cannot be delivered, stock that has run out. Because red is rare, it is unmistakable.
2. **Legible at 2 a.m.** Body text is 18 sp, touch targets are at least 48 dp, the confirm action is at least 56 dp tall, and every colour pair meets WCAG AA. The app must remain usable at 200 % system font scale.
3. **One tap to confirm.** The most important action on any surface is a single filled green button. Everything else steps back.
4. **The system's theme is the app's default, and the user's choice is final.** Settings offers System, Light and Dark. System is what an untouched install uses and follows the OS setting; the other two hold the app to one scheme whatever the phone does. Both schemes are first-class designs, not inversions of each other.
5. **Brand colour, not wallpaper colour.** Material You dynamic colour is switched off. Pillsner is always green, blue and white.

---

## 2. Colour

### 2.1 Brand hues

| Name | Hex | Role |
| --- | --- | --- |
| Pillsner Green | `#1B7F5C` | Primary. Confirmation, progress, "taken", the main action. |
| Pillsner Blue | `#2D6DA8` | Secondary. Navigation, information, "due", selected states. |
| Pillsner Teal | `#0F7C8C` | Tertiary. A bridge between green and blue used sparingly for "snoozed" and decorative accents. |
| Signal Red | `#BA1A1A` | Error. Danger and warnings only. |
| Mint White | `#F9FBFA` | Light surface. White with a trace of green so that pure white cards lift off it. |
| Deep Moss | `#101413` | Dark surface. Near-black with the same green bias. |

### 2.2 Material 3 colour roles

Pillsner uses the Material 3 colour role system unchanged so that every Material component picks up the brand without per-component overrides. The two schemes below are the complete token set. All "on" pairs meet at least 4.5 : 1; outlines meet at least 3 : 1.

| Role | Light | Dark |
| --- | --- | --- |
| primary | `#1B7F5C` | `#8CD8B0` |
| onPrimary | `#FFFFFF` | `#00382A` |
| primaryContainer | `#A8F0CE` | `#005E43` |
| onPrimaryContainer | `#00381F` | `#A8F0CE` |
| secondary | `#2D6DA8` | `#A2C9FF` |
| onSecondary | `#FFFFFF` | `#003259` |
| secondaryContainer | `#D3E4FF` | `#1D4E7F` |
| onSecondaryContainer | `#0B2F52` | `#D3E4FF` |
| tertiary | `#0F7C8C` | `#83D4E0` |
| onTertiary | `#FFFFFF` | `#00363D` |
| tertiaryContainer | `#B5EEF6` | `#005A66` |
| onTertiaryContainer | `#00363D` | `#B5EEF6` |
| error | `#BA1A1A` | `#FFB4AB` |
| onError | `#FFFFFF` | `#690005` |
| errorContainer | `#FFDAD6` | `#93000A` |
| onErrorContainer | `#410002` | `#FFDAD6` |
| surface | `#F9FBFA` | `#101413` |
| onSurface | `#191C1B` | `#E1E3E0` |
| surfaceVariant | `#DCE5E0` | `#3F4945` |
| onSurfaceVariant | `#3F4945` | `#BFC9C4` |
| surfaceContainerLowest | `#FFFFFF` | `#0B0F0E` |
| surfaceContainerLow | `#F3F6F4` | `#191C1B` |
| surfaceContainer | `#EDF1EF` | `#1D211F` |
| surfaceContainerHigh | `#E7ECE9` | `#272B29` |
| surfaceContainerHighest | `#E1E6E3` | `#323634` |
| outline | `#6F7975` | `#89938E` |
| outlineVariant | `#BFC9C4` | `#3F4945` |
| inverseSurface | `#2E312F` | `#E1E3E0` |
| inverseOnSurface | `#EFF1EE` | `#2E312F` |
| inversePrimary | `#8CD8B0` | `#1B7F5C` |
| scrim | `#000000` | `#000000` |

### 2.3 Intake state colours

Dose and intake states map onto the colour roles. Never introduce a new hue for a state.

| State | Meaning | Container / on-container | Icon |
| --- | --- | --- | --- |
| Due | Pending, not yet at or past its time | secondaryContainer / onSecondaryContainer | `schedule` |
| Taken | Confirmed by the user | primaryContainer / onPrimaryContainer | `check_circle` |
| Snoozed | "Not yet", reminder will return | tertiaryContainer / onTertiaryContainer | `snooze` |
| Skipped | "Not going to", deliberate, not a failure | surfaceContainerHighest / onSurfaceVariant | `remove_circle_outline` |
| Overdue | Past its time and still unanswered | errorContainer / onErrorContainer | `error` |
| Missed | Lapsed without an answer | errorContainer / onErrorContainer | `cancel` |

A skipped dose is neutral grey on purpose. The user chose it; the app does not scold.

### 2.4 Rules

- **Red is reserved.** Use `error` roles only for: overdue or missed doses, reminder permissions that are missing, destructive confirmations (delete a medicine), stock at zero, and validation errors. Never for emphasis, branding or "important" labels.
- **Green means done or go.** The filled primary button is the positive action. Do not use primary for decorative fills.
- **Blue means information and place.** Selected navigation item, due doses, links, informational chips.
- **Never hard-code a hex in a composable.** Read from `MaterialTheme.colorScheme`. The only file that knows a hex value is `ui/theme/Color.kt`.
- **Dynamic colour is off.** Do not call `dynamicLightColorScheme` or `dynamicDarkColorScheme`.
- **Text over images or gradients** is not used. Text always sits on a surface role.

---

## 3. Typography

### 3.1 Typefaces

| Role | Family | Weights bundled | Licence |
| --- | --- | --- | --- |
| Display and headlines | Raleway | 200 (ExtraLight), 300 (Light) | SIL Open Font License |
| Everything else | Montserrat | 400 (Regular), 500 (Medium), 600 (SemiBold) | SIL Open Font License |

Both families are bundled as static TTF files in `src/app/src/main/res/font/`. Downloadable fonts through Google Play services are not used: they need a network round trip and a Play dependency, both of which conflict with the privacy promise. Five static files cost roughly 1 MB; if that matters later, swap to the variable font of each family and select weights with `FontVariation.weight()`.

File names: `raleway_extralight.ttf`, `raleway_light.ttf`, `montserrat_regular.ttf`, `montserrat_medium.ttf`, `montserrat_semibold.ttf`.

### 3.2 Type scale

The default body style is **Montserrat 18 sp, weight 400**. The signature header style is **Raleway 48 sp, weight 200**. The rest of the scale is derived from those two anchors and uses Material 3 role names so that Material components inherit it.

| Material role | Family | Size sp | Weight | Line height sp | Letter spacing | Typical use |
| --- | --- | --- | --- | --- | --- | --- |
| displayLarge | Raleway | 48 | 200 | 56 | 0 | Screen title on Home ("Today"), welcome header |
| displayMedium | Raleway | 40 | 200 | 48 | 0 | Large time on the reminder screen |
| displaySmall | Raleway | 34 | 200 | 42 | 0 | Section openers |
| headlineLarge | Raleway | 32 | 300 | 40 | 0 | Screen titles on secondary screens |
| headlineMedium | Raleway | 28 | 300 | 36 | 0 | Dialog and sheet titles |
| headlineSmall | Raleway | 24 | 300 | 32 | 0 | Group headers ("Active", "Inactive") |
| titleLarge | Montserrat | 22 | 500 | 30 | 0 | Top app bar title, dose time on a tile |
| titleMedium | Montserrat | 18 | 600 | 26 | 0.1 | Medicine name on a tile |
| titleSmall | Montserrat | 16 | 600 | 24 | 0.1 | List item title, card subtitle |
| bodyLarge | Montserrat | 18 | 400 | 28 | 0.2 | **Default.** All running text, form values, tile details |
| bodyMedium | Montserrat | 16 | 400 | 24 | 0.2 | Secondary text, schedule description |
| bodySmall | Montserrat | 14 | 400 | 20 | 0.3 | Timestamps, helper text |
| labelLarge | Montserrat | 16 | 600 | 22 | 0.3 | Buttons, notification actions |
| labelMedium | Montserrat | 14 | 600 | 20 | 0.5 | Chips, bottom navigation labels |
| labelSmall | Montserrat | 12 | 600 | 16 | 0.5 | Badges, overline text |

### 3.3 Rules

- Raleway at weight 200 is used only at 34 sp and above. Below that, thin strokes lose contrast, so headline roles step up to weight 300. Never use Raleway under 24 sp.
- All sizes are in `sp`. Never set `fontScale` or clamp text size. Layouts must survive 200 % font scale by wrapping, not by truncating.
- Text is left-aligned. Centered text is limited to empty states and the welcome header.
- Numbers that line up in columns (times, quantities) use tabular figures: `fontFeatureSettings = "tnum"`.
- Times are rendered with the user's locale and 12/24-hour preference through `DateFormat`. Never format a time by hand.
- Maximum measure for running text is about 65 characters. On wide screens constrain content to 600 dp.

---

## 4. Shape

Material 3 shape scale with slightly generous corners. Soft, not bubbly.

| Token | Radius | Applied to |
| --- | --- | --- |
| extraSmall | 4 dp | Text field corners, tooltips |
| small | 8 dp | Chips, small containers |
| medium | 12 dp | Menus, snackbars |
| large | 16 dp | **Dose tiles and medicine tiles**, dialogs, the FAB |
| extraLarge | 28 dp | Bottom sheets |
| full | pill | Buttons, status chips, navigation indicator |

Buttons are fully rounded so they read as tappable at a glance. Cards are 16 dp so several can stack without looking like a pile of pills.

---

## 5. Spacing and layout

Everything sits on a 4 dp grid.

| Token | dp | Use |
| --- | --- | --- |
| Spacing.xs | 4 | Icon to label inside a chip |
| Spacing.sm | 8 | Between related lines inside a tile |
| Spacing.md | 12 | Chip padding, list item vertical padding |
| Spacing.lg | 16 | **Screen side padding**, gap between tiles, card inner padding |
| Spacing.xl | 24 | Between sections, below a screen title |
| Spacing.xxl | 32 | Above the first section on a screen |
| Spacing.xxxl | 48 | Empty state breathing room |

- Screen content has 16 dp horizontal padding on compact widths and 24 dp from medium width upwards.
- Content is constrained to a 600 dp column on tablets and foldables; the bottom navigation becomes a navigation rail at medium width and above.
- The bottom of every scrolling list gets extra padding equal to the FAB height plus 16 dp so the last tile is never hidden.
- Lists of tiles use `LazyColumn` with `Arrangement.spacedBy(16.dp)`.

---

## 6. Elevation and surfaces

Pillsner uses tonal elevation, not shadows. Depth comes from surface tiers.

| Level | Surface role | Use |
| --- | --- | --- |
| 0 | surface | Screen background |
| 1 | surfaceContainerLow | Inactive medicine tiles |
| 2 | surfaceContainer | Bottom navigation bar, top app bar when scrolled |
| 3 | surfaceContainerHigh | Bottom sheets, menus |
| Card | surfaceContainerLowest (light) / surfaceContainerHigh (dark) | **Dose and medicine tiles.** In light mode a tile is pure white on Mint White; in dark mode it is a lighter tier on Deep Moss. |

Shadows are permitted only on the FAB (Material default 6 dp) and on dialogs.

---

## 7. Iconography

- **Material Symbols Rounded**, 24 dp, weight 400, filled variant when the item is selected or the state is active, outlined otherwise.
- Every icon that stands alone has a `contentDescription`. Icons next to a label that says the same thing use `contentDescription = null`.
- The product mark is a placeholder until the final artwork lands: a rounded capsule split diagonally, green upper half, blue lower half, on a white circle. Adaptive icon foreground uses the same mark on a primary background.

Common mappings: `medication` (medicine), `schedule` (due), `check_circle` (taken), `snooze` (snoozed), `remove_circle_outline` (skipped), `error` (overdue), `cancel` (missed), `home`, `settings`, `add`, `notifications_off` (reminders unavailable).

---

## 8. Components

Each component names its Material 3 base and the tokens it uses. Anything not listed here inherits Material defaults through the theme and needs no local styling.

### 8.1 Dose tile

Shows one planned dose on the Home screen. `Card` with `large` shape.

```
┌──────────────────────────────────────────┐
│▌ [state icon]  Ibuprofen         08:00   │  titleMedium · titleLarge time
│▌               40 mg                     │  bodyLarge
│▌               ● Due                     │  status chip, labelMedium
└──────────────────────────────────────────┘
```

- Container: `surfaceContainerLowest` in light, `surfaceContainerHigh` in dark. No border.
- Left edge carries a 4 dp vertical stripe in the state's container colour so state is visible before the chip is read.
- Time is right-aligned, `titleLarge`, tabular figures.
- Padding 16 dp. Minimum height 72 dp. Whole tile is one accessibility node; its description reads "Ibuprofen, 40 milligrams, due at 8:00".
- When intake actions arrive, the tile grows a 56 dp filled "Taken" button along its bottom edge; the tile itself does not become the tap target.

### 8.2 Medicine tile

Shows one medicine on the Medicines screen. Same card as the dose tile without the stripe.

- Leading 40 dp circular icon avatar: `secondaryContainer` filled with the medication glyph for an active medicine, `surfaceContainerHighest` outlined for an inactive one. Purely decorative.
- Name in `titleMedium`; one `bodyMedium` line per schedule description; "As needed · 40 mg" when there are none.
- Trailing chevron when the tile opens the medicine's details, hinting the tap target without adding words to the spoken description.
- Inactive medicines: container `surfaceContainerLow`, all text in `onSurfaceVariant`, a small `Inactive` chip (`surfaceContainerHighest`), and `stateDescription = "Inactive"` for TalkBack. Do not lower alpha; it fails contrast.

### 8.3 Status chip

`AssistChip`-shaped, non-interactive, `full` shape, 28 dp tall, `labelMedium`, 8 dp icon-to-text gap, 12 dp horizontal padding. Colours from section 2.3. Always includes the icon so state does not rely on colour alone.

### 8.4 Buttons

| Kind | Material | Use |
| --- | --- | --- |
| Filled | `Button` | The one positive action: Save, I took it, Add medicine |
| Tonal | `FilledTonalButton` | Second-rank action: Not yet, Edit |
| Outlined | `OutlinedButton` | Neutral alternatives: Cancel, Not going to |
| Text | `TextButton` | Low-emphasis in dialogs and sheets |
| Destructive | `Button` with `error` container | Delete medicine, only inside a confirmation dialog |

Minimum height 48 dp; the primary confirm on reminder and dose surfaces is 56 dp. Label in `labelLarge`, sentence case, verb first. Full-width on compact screens when the button is the screen's main action.

### 8.5 Floating action button

`FloatingActionButton` at its standard 56 dp - not the medium or large variant, which covered the last tile on a compact screen - `primaryContainer` / `onPrimaryContainer`, `add` icon, `contentDescription = "Add medicine"`. Bottom-end, 16 dp from edges, above the navigation bar.

### 8.6 Bottom navigation

`NavigationBar` on `surfaceContainer`. Three items: Home, Medicines, Settings. Selected item: filled icon, `secondaryContainer` indicator pill, label in `onSurface`. Unselected: outlined icon, `onSurfaceVariant`. Labels always visible. At medium width and above this becomes `NavigationRail`.

### 8.7 Top app bar

Screens with a display title (Home) render the title in content as `displayLarge`; they have no app bar. Secondary screens use `TopAppBar` with the title in `titleLarge`, a back arrow, and a `surface` container that shifts to `surfaceContainer` on scroll. `CenterAlignedTopAppBar` is not used.

### 8.8 Reminder notification

Built with `NotificationCompat`; the system renders it, so the design lives in content and actions.

- Small icon: monochrome capsule mark. Accent colour: Pillsner Green.
- Title: medicine name. Text: "Take 40 mg of your medicine 'Ibuprofen', on 08:00".
- Actions in this order: **I took it**, **Not yet**, **Not going to**. Order is fixed so muscle memory works.
- Public (lock-screen) version: "Time for your medicine", no name, no amount.
- Full-screen reminder (if a change adds one): time in `displayMedium`, medicine in `headlineMedium`, amount in `bodyLarge`, a 56 dp filled button, tonal and outlined buttons below it, all stacked and full width.

### 8.9 Attention banner

Used when reminders cannot be delivered. `errorContainer` / `onErrorContainer`, `large` shape, `notifications_off` icon, one `bodyLarge` sentence explaining the problem, one `TextButton` in `onErrorContainer` that opens the system setting. Sits at the top of Home above the dose list. This is the only red surface that appears without user action.

### 8.10 Empty state

Centered in the remaining space. Outlined 64 dp icon in `onSurfaceVariant`, `headlineSmall` line in `onSurface`, `bodyLarge` hint in `onSurfaceVariant`, optional filled button. Example: "Nothing due right now" / "Your next dose will appear here."

### 8.11 Forms

- `OutlinedTextField` with `bodyLarge` value, `bodyMedium` label, `bodySmall` supporting text. Error state uses the `error` role and a message that says how to fix it.
- Quantity fields pair a numeric text field with an `ExposedDropdownMenuBox` for the unit.
- Dates open the Material `DatePickerDialog`; times open `TimePickerDialog`. Never a free-text time.
- Weekdays are `FilterChip`s in a wrapping row, selected chips in `secondaryContainer`.
- The Save action is a filled button pinned to the bottom of the form, above the keyboard.

### 8.12 Dialogs and sheets

`AlertDialog` for confirmations, `headlineMedium` title, `bodyLarge` body, at most two actions. `ModalBottomSheet` for pickers and editors that need more room, on `surfaceContainerHigh`, `extraLarge` top corners, with a drag handle.

### 8.13 Wordmark

The app name is a blend of **Pills** and Part**ner**, and the wordmark shows it. `Pills` is drawn in `secondary`, `ner` in `primary`, in both schemes. Rules:

- One word, no gap. The fragments sit flush against each other, at one type role and one weight. It is a colour split, not two words.
- One text node. Built as a single `Text` over an `AnnotatedString` with a `SpanStyle` per fragment, so TalkBack, text selection and test matchers all see the single word "Pillsner". Never a `Row` of two `Text`s.
- Decoration only. The colouring carries no meaning, so nothing is lost when it is not perceived.
- `displayLarge` by default, and never below `headlineSmall`: the display roles use Raleway at weight 200, which section 3.3 forbids under 24 sp.
- The name comes from the `app_title` string resource and the split is derived from it. A title that does not end in `ner` is drawn whole in `secondary`.

Implemented once, in `ui/components/PillsnerWordmark.kt`. Every surface that shows the name uses it; no screen re-implements the split.

### 8.14 Website header navigation

The marketing website (`src/website/`) is not Compose, but it follows the same tokens through `assets/css/tokens.css`, and its header is the web counterpart of the app's navigation (8.6). One `<nav>` is rendered per page and presented two ways.

**Structure.** At most four top-level entries, never more: **Home**, **Features** (Medicines, Schedules, Tracking, On your wrist), **Privacy**, **Help** (FAQ, Report a bug, Request a feature). Groups are declared once in `hugo.toml` with the menu `parent` field. A new page joins a group; it does not become a fifth top-level entry. Top-level labels stay one or two words in every locale.

**Wide (960 px and up).** One row inside the 1080 px content column, on `surfaceContainer` with a 1 px `outlineVariant` bottom border, sticky at the top.

- Start: the product mark (32 px, `medium` corners) and the name in Raleway 300, 22 px.
- Centre: the four entries as pills - `labelLarge`, `onSurfaceVariant`, 40 px tall, `full` shape, `Spacing.md` / `Spacing.lg` padding, `Spacing.xs` between pills, 48 px hit area.
- Hover and keyboard focus: `secondaryContainer` background, `onSecondaryContainer` text; focus also shows the site's standard 3 px `primary` focus ring.
- Current page, and the group that contains it: `secondaryContainer` pill with `onSecondaryContainer` text, plus `aria-current="page"` on the link. Blue means place, as in the app.
- Groups show a 16 px `chevron-down` after the label that rotates 180° when open. They open a dropdown on `surfaceContainerHigh`, `medium` shape, the language switcher's shadow, at least 220 px wide, `Spacing.xs` inner padding, items `bodyMedium` 48 px tall with `small` corners and the same hover and current treatment. Only one group is open at a time; Escape or a click outside closes it.
- End: the language switcher (outlined pill, unchanged).

**Narrow (below 960 px).** The bar holds only the mark and name, the language switcher and a 48 × 48 px **Menu** icon button (`menu` icon, `onSurface`, `full`-shape hover layer). Activating it swaps the icon to `close`, changes its accessible name to "Close menu", sets `aria-expanded="true"` and opens a full-width panel directly under the bar:

- `surfaceContainer`, `Spacing.lg` side gutters, `Spacing.sm` above and `Spacing.lg` below, `outlineVariant` bottom border.
- Every group starts expanded, so every page is listed at once; the group heading keeps its chevron and can still collapse it. Group headings (Features, Help) in `labelSmall`, uppercase, `onSurfaceVariant`, `Spacing.sm` above each, 48 px tall. A top-level entry that follows a group (Privacy) sits below a 1 px `outlineVariant` divider so it does not read as part of the group.
- Links in `bodyLarge`, `onSurface`, 48 px tall, `medium` corners; current page gets the `secondaryContainer` treatment.
- The panel scrolls on its own when taller than the viewport; the page behind does not move.
- Escape, the Menu button or following a link closes it, and focus returns to the Menu button.

**Motion.** Dropdown and panel enter with the short duration (150 ms) as a fade plus a 4 px downward slide, and leave without animation. With `prefers-reduced-motion: reduce` there is no animation at all.

**Without JavaScript.** Groups are native `<details>`/`<summary>`, so they open and close with no script. The Menu button is hidden and the narrow-screen menu is shown in place as a stacked list, with the groups collapsed and the header no longer sticky, so every page stays reachable without covering the screen. A script only adds the toggle, outside-click and Escape behaviour.

**Never.** No red in navigation, no icons in front of menu labels, no hover-only dropdowns, no truncated labels, no horizontally scrolling menu.

---

## 9. Motion

- Durations: 150 ms short (chips, toggles), 250 ms medium (tiles appearing, state change), 350 ms long (screen transitions). Material `EmphasizedDecelerate` for entering, `EmphasizedAccelerate` for leaving.
- Screen transitions: shared-axis horizontal between top-level destinations, fade-through into forms.
- Marking a dose taken: the stripe and chip cross-fade from Due to Taken; no confetti, no bounce. Calm.
- Respect the system "Remove animations" setting: when the animator duration scale is 0, all durations become 0.

---

## 10. Accessibility

- Contrast: 4.5 : 1 for all text, 3 : 1 for icons, outlines and large text. The token tables above are verified; do not tint tokens ad hoc.
- Touch targets: 48 × 48 dp minimum, 56 dp for the primary confirm.
- Font scale: all screens tested at 100 %, 150 % and 200 %. Text wraps, never truncates, except the top app bar title.
- TalkBack: every tile is a single node with a complete description; state chips carry `stateDescription`; every icon-only control has a `contentDescription`; heading roles are set with `semantics { heading() }`.
- Colour is never the only carrier of meaning. Every state has an icon and a label.
- One-handed use: the primary action on every screen sits in the bottom third.
- Reduce motion is honoured (section 9).

---

## 11. Implementation in Compose

All theme code lives in `src/app/src/main/java/.../ui/theme/`.

| File | Contents |
| --- | --- |
| `Color.kt` | Every hex from section 2.2 as `val` constants, plus `LightColorScheme` and `DarkColorScheme` built with `lightColorScheme()` / `darkColorScheme()`. The only file in the app that contains a hex literal. |
| `Type.kt` | `RalewayFamily`, `MontserratFamily` (from `R.font.*`) and `PillsnerTypography` implementing every row of section 3.2. |
| `Shape.kt` | `PillsnerShapes` from section 4. |
| `Dimens.kt` | `object Spacing` with the section 5 tokens as `Dp` values and touch-target minimums. |
| `IntakeStatusColors.kt` | `@Composable fun intakeStatusColors(status: IntakeStatus): StatusColors` returning container, on-container and icon per section 2.3 from `MaterialTheme.colorScheme`. |
| `Theme.kt` | `@Composable fun PillsnerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content)`. Picks the scheme, applies typography and shapes, sets status and navigation bar appearance through `enableEdgeToEdge`. No dynamic colour parameter. `MainActivity` passes the user's stored theme choice resolved against the phone; the default follows the phone and is what previews and tests use. |

Every screen is wrapped in `PillsnerTheme` in `MainActivity` once. Previews use `@PreviewLightDark` so both themes render side by side in Android Studio.

Composables read tokens through `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes` and `Spacing`. A composable that needs a colour, size or font that is not a token is a signal that either the design system or the composable is wrong; fix one of them rather than inlining a value.

---

## 12. Do and don't

| Do | Don't |
| --- | --- |
| Use `displayLarge` once per screen, for the title | Stack several Raleway headers on one screen |
| Put the confirm action at the bottom, full width, 56 dp | Hide confirm behind a menu or a swipe |
| Show state with stripe, icon and label | Rely on colour alone |
| Use `errorContainer` for overdue and missed | Use red to make a heading "stand out" |
| Read colours from `MaterialTheme.colorScheme` | Write `Color(0xFF1B7F5C)` in a screen |
| Bundle fonts in `res/font` | Use downloadable fonts |
| Let text wrap at large font sizes | Set `maxLines = 1` on anything but the app bar title |
| Render the user's stored theme choice, defaulting to the system setting | Add a fourth theme, a schedule or an AMOLED variant without a proposal |
| Keep dynamic colour off | Call `dynamicLightColorScheme` |
| Add a new website page to the Features or Help group | Add a fifth top-level entry to the website header |
