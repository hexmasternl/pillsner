## Context

The welcome screen header (`ui/home/WelcomeHeader.kt`) draws the app title as one `Text` reading `stringResource(R.string.app_title)` in `displayLarge` (Raleway 48 sp, weight 200) and the inherited `onSurface` colour. It is the only place in the app, the wear module or the shared module that renders the name. The design system fixes the type role and centring for the welcome header (sections 3.2, 3.3, 8.7) and forbids hex values outside `ui/theme/Color.kt`.

The name is a blend: **Pills** + Part**ner**. The change gives the two fragments different brand colours so the blend is visible. Nothing about layout, size, weight, alignment, wrapping or reading order changes.

There is no `ui/components` package yet; every shared-looking composable so far lives beside its screen. This change creates that package, because the wordmark is by definition not owned by Home.

## Goals / Non-Goals

**Goals:**

- Render the title as `Pills` in `secondary` and `ner` in `primary`, in both light and dark schemes, using theme tokens only.
- Keep the title a single, unbroken visual word: one `Text`, one line, no inserted space, no per-fragment padding.
- Keep the title a single accessibility node announced as "Pillsner", and keep it findable by existing text and tag matchers.
- Put the wordmark in one reusable composable so a later About or splash screen cannot drift from it.
- Document the treatment in the design system so it is a rule, not an accident.

**Non-Goals:**

- Changing the logo drawable, the header layout, spacing, or the `displayLarge` role.
- Applying the two-colour treatment to the launcher label, the notification title, the app bar on secondary screens, or anywhere the name appears inside a sentence.
- Introducing a second font weight or size to separate the fragments. The type scale stays as it is.
- Any localisation of the wordmark. `app_title` is `translatable="false"`; the name is the name in every language.

## Decisions

### 1. One `Text` with an `AnnotatedString`, not two composables

`buildAnnotatedString` with a `SpanStyle(color = …)` per fragment keeps a single text node. Two `Text` composables in a `Row` would split the accessibility node in two, break text selection and copy, and introduce a hairline gap that is visible at 48 sp. It would also break at large font scales, where a `Row` cannot wrap mid-word.

The annotated string is built once from the string resource; the composable receives no fragment parameters.

**Alternative rejected:** a `Row` of two `Text`s, or a `Text` with two `withStyle` children inside a `Row` — both lose the single-node property this change promises to keep.

### 2. Colours: `Pills` = `secondary`, `ner` = `primary`

Chosen by the product owner over the green-then-blue ordering. The reasoning is that the design system reserves green for "done or go" (section 2.4) and this keeps the primary green rarer on a screen whose main action is a green confirm button; giving the leading fragment blue avoids a decorative green that competes with it.

Consequence to accept knowingly: the emphasis on `Pills` is carried by colour distinction and word order alone, not by hierarchy — both fragments share one type role, and blue is not visually louder than green here. If a stronger lead is wanted later, that is a separate design change, not a weight or size override in this composable.

Contrast: at 48 sp the text is "large" under WCAG, so the 3 : 1 threshold applies. Light `secondary` `#2D6DA8` and `primary` `#1B7F5C` on `surface` `#F9FBFA` and dark `#A2C9FF` / `#8CD8B0` on `#101413` all clear it comfortably, and in fact clear 4.5 : 1.

**Alternative rejected:** `primary` + `onSurfaceVariant`, which gives the strongest hierarchy but drops half the wordmark out of brand colour.

### 3. Split point derived from the resource, not hard-coded fragments

The composable takes the full title string and splits it at the last occurrence of the accent fragment (`"ner"`, a private constant), colouring everything before it as the lead. If the resource does not end with that fragment — someone renames the app, or a stray translation appears — the whole string is rendered in the lead colour and nothing crashes or renders half-empty. This keeps `app_title` the single source of the name and makes the failure mode boring.

**Alternative rejected:** two new string resources (`app_title_lead`, `app_title_accent`). That splits the app name across two resources that can disagree, and a translator or a careless edit can produce "Pills" + "ners".

### 4. Placement: `ui/components/PillsnerWordmark.kt`

New package `nl.hexmaster.pillsner.ui.components`. Not `ui/theme` — the theme package holds tokens and the theme wrapper, and a composable that renders content does not belong there. Not `ui/home` — Home is one caller, not the owner.

The composable exposes `modifier` and a `style` defaulting to `MaterialTheme.typography.displayLarge`, so a future About screen can render the same wordmark at `headlineLarge` without a copy. Colours are not parameters; they are the wordmark's definition.

### 5. Accessibility and testability

`WelcomeHeader` keeps its `semantics { heading() }` on the title and its existing `WelcomeScreenTestTags.HEADER` tag on the column. Because the node's text is still `"Pillsner"`, TalkBack announces one word, `onNodeWithText("Pillsner")` still matches, and the two existing `WelcomeScreenTest` assertions on `HEADER` are unaffected. Colour is decoration; no information is conveyed by it, which is what section 2.4 and the "status never by colour alone" rule require.

### 6. Verification

A unit test on the split function (a pure Kotlin helper, no Compose) covers: the real title splits into `Pills` / `ner`; a string without the accent fragment yields a single lead fragment; a string that is exactly the accent fragment does not yield an empty lead that would render nothing. A Compose test asserts the header still exposes one node with the text "Pillsner". `@PreviewLightDark` previews on the wordmark and the existing header preview give the visual check in both schemes. `pillsner-ui-review` runs before the UI task is called done.

## Risks / Trade-offs

- **The two-colour name reads as a typo or a broken render to someone who does not know the story.** → The fragments are adjacent with no gap, share one type role, and use two brand hues that already sit together across the app, so it reads as styling rather than damage. The design system entry records the intent.
- **`Pills` does not visually dominate, which is what was originally asked for.** → Accepted deliberately (decision 2). Recorded here so the next person does not "fix" it by bolding a fragment and breaking the type scale.
- **Hard-coding the accent fragment `"ner"` couples the composable to the app name.** → It is a private constant in one file with a documented fallback, and the app name is `translatable="false"`. The fallback means a rename degrades to a plain title rather than a crash.
- **A future caller renders the wordmark at a size below 24 sp, where Raleway weight 200 loses contrast.** → The `style` parameter defaults to `displayLarge`; the KDoc states the design-system rule that Raleway is never used under 24 sp, and `pillsner-ui-review` catches violations at review time.
