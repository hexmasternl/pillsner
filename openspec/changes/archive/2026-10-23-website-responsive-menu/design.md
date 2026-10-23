## Context

GitHub issue [#63](https://github.com/hexmasternl/pillsner/issues/63). `website-multipage-expansion` added nine pages and a header that renders `.Site.Menus.main` as one flat, `flex-wrap: wrap` row (`layouts/partials/header.html`, `.site-nav` in `assets/css/layout.css`). With labels such as "Proposer une fonctionnalité" or "Funktion vorschlagen" the row needs roughly twice the 1080 px container, so it wraps into ragged lines on desktop, and on a phone the sticky header grows to several rows of links.

Constraints carried over from the existing site and spec:

- Static Hugo site, no runtime server, no third-party scripts, no tracking. The only client storage is the language choice.
- Everything must work without JavaScript; the language switcher already uses `<details>`/`<summary>` for that reason.
- Colours, type, spacing and shapes come from `assets/css/tokens.css`, which mirrors `docs/design-system.md`.
- One menu definition in `hugo.toml`, labels from `i18n/<lang>.toml` keyed `nav_<identifier>`, English fallback.

## Goals / Non-Goals

**Goals:**
- A header that is one tidy row on wide screens and a single Menu button on narrow screens, in every locale.
- Keyboard, screen reader and 48 px touch-target support equal to the rest of the site.
- Documented in the design system so later website work follows the same pattern.

**Non-Goals:**
- Changing which pages exist, their URLs or their content.
- Changing the language switcher's behaviour (only its placement within the header).
- A mega-menu, search, or icons per menu item.
- Any change to the Android app.

## Decisions

### Group the pages into four top-level entries

Home · **Features** (Medicines, Schedules, Tracking, On your wrist) · Privacy · **Help** (FAQ, Report a bug, Request a feature).

Features are the four product pages; Help is everything a visitor reaches for when they need something. Privacy stays top-level because it is the product's differentiator and the site already promotes it on the home page.

*Alternatives:* a "More" overflow that moves whatever doesn't fit — rejected, the contents would differ per locale and per window width, which makes the site unpredictable and needs JS measuring. Shortening labels — rejected, nine entries do not fit even with short labels, and translations would suffer.

### Declare groups with Hugo's menu `parent`

Two new entries in `hugo.toml` (`identifier = "features"`, `identifier = "help"`, no `pageRef`) and `parent = "features"` / `parent = "help"` on the child entries. The template uses `.HasChildren`, `.Children`, `$page.IsMenuCurrent` and `$page.HasMenuCurrent`. `nav_features` already exists in every locale (left from the single-page site); `nav_help` is new.

The footer keeps its flat "Explore" list by iterating top-level entries and, for a parent, its children.

### One markup, two presentations, switched at 960 px

The same `<nav>` is rendered once. CSS below 960 px hides it behind the Menu button and shows it as a panel; from 960 px it is an inline row. 960 px leaves room for brand (~140 px), four entries in the longest locale (~560 px in French) and the language switcher (~150 px) inside the 1080 px container with 24 px gutters. The breakpoint lives in one place in `layout.css` with a comment pointing at the design system.

*Alternative:* two navs (desktop and mobile) — rejected, it duplicates links for screen readers and doubles the template.

### Menu button is a real `<button>`, progressively enhanced

- Without JS (`html` lacks the `js` class): no Menu button is shown and the nav renders in place as a stacked list on narrow screens, with the groups as collapsed disclosures and the header no longer sticky (so it cannot cover a phone screen). Every link is reachable.
- With JS: `assets/js/navigation.js` adds the `js` class on `<html>`, reveals the button, and toggles `aria-expanded`, the button's `aria-label` ("Menu" / "Close menu" from i18n, passed through `data-` attributes) and the panel's visibility. It closes the panel on Escape (returning focus to the button), on a link click, and when a `matchMedia("(min-width: 960px)")` change fires.

The `js` class is set by a one-line inline script in `<head>` so there is no flash of the expanded no-JS list. The CSP-free static hosting allows this; it contains no data and no network calls.

*Alternative:* a `<details>` element for the Menu button too — rejected, the panel must be always-visible on wide screens and a closed `<details>` cannot be forced open with CSS in all target browsers.

### Group dropdowns use `<details>`/`<summary>`

Same pattern as the language switcher: works without JS, keyboard operable, announced as expandable. `navigation.js` adds closing on outside click, on focus leaving the group and on Escape (returning focus to the summary), and closes a sibling group when another opens. Inside the narrow-screen panel, `navigation.js` opens every group whenever the narrow layout is active and closes them all when the wide layout takes over, so a phone visitor sees all pages at once. The summaries stay real, collapsible disclosure controls styled as group headings, rather than being made non-interactive: that keeps their semantics honest for screen readers and needs no tab-order tricks.

### Visual treatment

- Header bar on `surfaceContainer` with an `outlineVariant` bottom border, as today.
- Top-level links and summaries: `labelLarge`, `onSurfaceVariant`, 40 px tall pill (`shape-full`) with `space-md`/`space-lg` padding inside a 48 px hit area. Hover and focus: `secondaryContainer` at hover tint, `onSecondaryContainer`. Current page, or group containing it: `secondaryContainer` pill, `onSecondaryContainer` text — the web equivalent of the app's navigation indicator (design system 8.6).
- Dropdown: `surfaceContainerHigh`, `shape-medium`, same shadow as the language switcher menu and a 150 ms entrance, minimum width 220 px, items `bodyMedium`, 48 px tall.
- Mobile panel: full width under the sticky header, `surfaceContainer`, `space-lg` gutters, group headings in `labelSmall` uppercase `onSurfaceVariant`, links `bodyLarge` 48 px tall, max-height `calc(100dvh - header height)` with `overflow-y: auto`.
- Menu button: 48 × 48 px icon button, `menu` icon swapping to `close`, `onSurface`, `shape-full` hover state layer.
- Motion: 150 ms fade/slide, disabled under `prefers-reduced-motion: reduce`.
- No red anywhere in navigation.

These rules are recorded as design system section 8.14 *Website header navigation* in `docs/design-system.md`, with a light and dark specimen in `docs/design-system.html`.

## Risks / Trade-offs

- [A future page added to the menu without a `parent`] → it becomes a fifth top-level entry and may break the one-row guarantee. Mitigation: a comment in `hugo.toml` next to the menu, and the design system section states the four-entry limit.
- [A translation makes a top-level label much longer] → the one-row check at 960 px is part of the verification tasks for every locale; the design system section asks new top-level labels to stay one or two words.
- [Inline `<head>` script] → it only sets a class; if a strict CSP is ever added it needs a hash. Noted in a template comment.
- [Grouping hides five pages behind one extra click on desktop] → the footer and the home page's feature summary still link every page directly.

## Migration Plan

Static site; merge to `development`, then released with the next `development` → `main` merge, which redeploys through `website.yml`. Rollback is reverting the merge. No URL changes, so no redirects.

## Open Questions

None.

## Implementation notes

- **Old navigation rules removed.** `assets/css/pages.css` still carried the `website-multipage-expansion` stopgap (a sideways-scrolling nav below 900 px and a green `primary` current-page link). It overrode the new current-page colours, so it is removed; all header navigation styling now lives in `assets/css/layout.css`, including the dropdown (task 4.2 therefore touched `layout.css`, not `components.css`).
- **Divider after a group.** On narrow screens, Privacy directly after the Features group read as that group's fifth item. A top-level entry that follows a group now gets a 1 px `outlineVariant` divider (`.site-nav__item--group + .site-nav__item`).
- **`--shape-small` token.** The design system's `small` (8 dp) shape had no CSS token; the language switcher used an inline fallback. `tokens.css` now defines `--shape-small: 8px` and both menus use it.
