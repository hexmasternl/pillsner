## Context

`Theme.Pillsner` (`src/app/src/main/res/values/themes.xml`, `values-night/themes.xml`) sets `android:statusBarColor`, `android:navigationBarColor` and `android:windowLightStatusBar` directly on the theme. This dates from before `enableEdgeToEdge()` and the `WindowInsetsControllerCompat`-based bar styling in `ui/theme/Theme.kt` were added; both now already do this job correctly at runtime, so the theme attributes are leftover and, per Android 15's edge-to-edge enforcement, deprecated. Play Console's pre-launch report surfaces this as two warnings (GitHub #14 and the now-closed duplicate #15).

## Goals / Non-Goals

**Goals:**
- Remove the deprecated, redundant theme attributes so Play Console stops flagging both warnings.
- Confirm bar appearance (transparent, correct light/dark icon contrast) and content-not-obscured behavior are unaffected, since `enableEdgeToEdge()` + `WindowInsetsControllerCompat` already own this behavior.

**Non-Goals:**
- No change to `MainActivity.kt`'s `enableEdgeToEdge()` call or its placement — it is already correct.
- No change to `ui/theme/Theme.kt`'s `WindowInsetsControllerCompat` usage — it is already correct.
- No broader theming, insets, or Compose layout refactor. Per-screen inset handling (`Scaffold`/`NavigationSuiteScaffold` insets) is out of scope unless verification (task list) finds a real regression.

## Decisions

- **Remove rather than replace the deprecated attributes.** `android:statusBarColor` / `android:navigationBarColor` have no non-deprecated theme-XML equivalent for edge-to-edge apps — the replacement is exactly what `enableEdgeToEdge()` already does at runtime (transparent scrim styles), so removing the attributes (rather than swapping to some other XML value) is the correct fix, not a partial one.
- **Keep `android:windowLightStatusBar` out of the theme, not just retarget it.** `WindowInsetsControllerCompat.isAppearanceLightStatusBars` in `Theme.kt` already sets this per-frame based on `darkTheme`, which is more correct than a static theme value anyway (it reacts to the user's in-app theme choice, not just system dark mode — see `app-theme` spec D4/D6 already covered by that capability). Leaving the static attribute in place would just be dead weight, not a functional fallback, since Compose always renders through `PillsnerTheme` before any content is shown.
- **Keep `android:windowBackground`.** It paints the surface color behind the very first frame before Compose has run; it is unrelated to the deprecated bar-color attributes and has no edge-to-edge implication.
- **No new runtime code planned up front.** Because `enableEdgeToEdge()` and the `WindowInsetsControllerCompat` calls already exist and are correctly placed, the fix is expected to be a pure theme-XML deletion. Verification (manual, on-device) is the safeguard against a hidden dependency on the removed attributes.

## Risks / Trade-offs

- [Some OEM skin or older Android version relies on the theme-level `statusBarColor`/`navigationBarColor` as a fallback before `enableEdgeToEdge()` takes effect, causing a brief flash of a non-transparent bar] → Mitigation: manually verify on both an API 35+ device/emulator and at least one pre-35 device/emulator (down to `minSdk` 26) before considering this done; if a flash is observed, that becomes a follow-up finding rather than blocking this change, since `enableEdgeToEdge()` is documented to handle this across the app's supported SDK range.
- [Removing `android:windowLightStatusBar` changes the *default* theme value, but `Theme.kt` always overrides it before first paint] → Mitigation: confirm via manual testing that there is no visible gap/flash between window creation and the first Compose frame's `SideEffect`.

## Migration Plan

Single, non-breaking XML edit; no data, schema, or dependency migration involved. No rollback plan beyond reverting the two `themes.xml` files if manual verification surfaces a regression.

## Open Questions

None outstanding — the scope is confirmed by reading `MainActivity.kt`, `ui/theme/Theme.kt`, and both `themes.xml` files.
