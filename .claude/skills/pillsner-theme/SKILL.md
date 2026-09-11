---
name: pillsner-theme
description: Create or verify the Pillsner Compose theme layer (ui/theme) from docs/design-system.md - colour schemes, typography with bundled Montserrat and Raleway, shapes, spacing tokens, intake status colours and PillsnerTheme. Use when the project has no theme yet, when a token needs adding or changing, or when checking that ui/theme matches the design system.
metadata:
  author: pillsner
  version: "1.0"
---

Implement or verify the theme layer so that every other composable can rely on tokens alone.

**Source of truth:** `docs/design-system.md`, sections 2 (colour), 3 (typography), 4 (shape), 5 (spacing), 11 (implementation). Read them before writing. If this skill and the document disagree, the document wins; fix this skill afterwards and say so.

**Package:** `nl.hexmaster.pillsner.ui.theme` under `src/app/src/main/java/`.

## Steps

1. **Check what exists.** Glob `src/app/src/main/java/**/ui/theme/*.kt` and `src/app/src/main/res/font/*`. If `src/` has no Gradle project yet, stop and say so: the theme is created by the change that scaffolds the app (`app-welcome-screen`), and this skill supplies the code for that task.

2. **Fonts.** The five static TTF files must exist in `res/font/` with these exact names (lowercase, underscores, Android resource rules):
   `raleway_extralight.ttf`, `raleway_light.ttf`, `montserrat_regular.ttf`, `montserrat_medium.ttf`, `montserrat_semibold.ttf`.
   Both families are SIL Open Font License. Download the static files from the official Google Fonts repositories (`github.com/googlefonts/raleway` and `github.com/JulietaUla/Montserrat`, `fonts/ttf/`) and add a `res/font/OFL.txt` with the licence text. Never use `androidx.core:core-google-shortcuts` or downloadable fonts: no network, no Play dependency.

3. **Write the six files** from `reference/theme-code.md` in this skill. Copy them exactly, then adjust only the package line if the project package differs. The files are:
   - `Color.kt` - every hex from section 2.2 and the two `ColorScheme`s. The only file in the app allowed to contain a hex literal.
   - `Type.kt` - the two `FontFamily`s and `PillsnerTypography`, every row of section 3.2.
   - `Shape.kt` - `PillsnerShapes`.
   - `Dimens.kt` - `Spacing` and touch-target constants.
   - `IntakeStatusColors.kt` - `IntakeStatus` to container / on-container / icon mapping (section 2.3).
   - `Theme.kt` - `PillsnerTheme` following `isSystemInDarkTheme()`, no dynamic colour, edge-to-edge system bars.

4. **Verify** with these checks and report each result:
   - `grep -rn "0xFF" src/app/src/main/java --include=*.kt | grep -v ui/theme/Color.kt` returns nothing.
   - `grep -rn "dynamicLightColorScheme\|dynamicDarkColorScheme" src/app/src/main/java` returns nothing.
   - `Type.kt` has exactly the sizes 48, 40, 34, 32, 28, 24, 22, 18, 16, 14, 12 and no Raleway style under 24 sp.
   - `PillsnerTheme` has a `darkTheme: Boolean = isSystemInDarkTheme()` parameter and no `dynamicColor` parameter.
   - A `@PreviewLightDark` preview in `ThemePreview.kt` renders a `Text` in `displayLarge`, one in `bodyLarge`, one filled `Button` and each `IntakeStatus` chip so both palettes can be eyeballed in Android Studio.
   - Run `./gradlew :app:lintDebug :app:testDebugUnitTest` from `src/` when the project builds. Report output verbatim.

5. **Adding or changing a token** (later in the project): change `docs/design-system.md` first through the OpenSpec change that needs it, then mirror the change in `ui/theme`, then re-run the contrast check below. A token that is only in code is a defect; a token that is only in the document is a to-do.

## Contrast check

Any new colour pair must clear 4.5 : 1 for text and 3 : 1 for icons and outlines. Compute it rather than guessing:

```bash
node -e '
const lum=h=>{h=h.replace("#","");const [r,g,b]=[0,2,4].map(i=>parseInt(h.substr(i,2),16)/255);const f=c=>c<=0.03928?c/12.92:Math.pow((c+0.055)/1.055,2.4);return .2126*f(r)+.7152*f(g)+.0722*f(b)};
const cr=(a,b)=>{const la=lum(a),lb=lum(b);return (Math.max(la,lb)+.05)/(Math.min(la,lb)+.05)};
console.log(cr(process.argv[1],process.argv[2]).toFixed(2))' "#1B7F5C" "#FFFFFF"
```

## What not to do

- Do not add a theme toggle, a `dynamicColor` flag or a "use system colours" setting. Section 1, principle 5.
- Do not derive a scheme with Material Theme Builder and paste its output; the tokens in section 2.2 are hand-checked and differ from the generated ones.
- Do not put spacing or size constants anywhere except `Dimens.kt`.
- Do not create extra `CompositionLocal`s for colours; intake status colours are derived from `MaterialTheme.colorScheme` inside a composable function.
