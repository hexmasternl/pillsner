## Why

**GitHub Issue:** #14 (https://github.com/hexmasternl/pillsner/issues/14)

Google Play's pre-launch report flags two Android 15 (API 35+) edge-to-edge warnings for Pillsner: "Edge-to-edge may not display for all users" (#14) and "Your app uses deprecated APIs or parameters for edge-to-edge" (#15, closed as a duplicate of #14). Investigation found a single root cause: `Theme.Pillsner` still sets `android:statusBarColor`, `android:navigationBarColor` and `android:windowLightStatusBar` directly in `themes.xml` and `values-night/themes.xml`. These attributes are deprecated for edge-to-edge apps on API 35+ (this app compiles against and targets API 37) and are redundant with the runtime bar handling `ui/theme/Theme.kt` already performs correctly via `WindowInsetsControllerCompat`. Fixing this now keeps the app compliant with Play Store policy and avoids inconsistent bar rendering across OS versions and OEMs.

## What Changes

- Remove `android:statusBarColor`, `android:navigationBarColor` and `android:windowLightStatusBar` from `src/app/src/main/res/values/themes.xml` and the equivalent attributes from `src/app/src/main/res/values-night/themes.xml`, keeping only `android:windowBackground` on `Theme.Pillsner`.
- Keep `MainActivity`'s existing `enableEdgeToEdge()` call (already correctly placed before `super.onCreate()`) and `Theme.kt`'s existing `WindowInsetsControllerCompat` usage as the sole source of status/navigation bar appearance (light/dark icon contrast) — no new runtime code needed unless verification finds a gap.
- Manually verify, on a device or emulator running Android 15+ and on at least one pre-15 device/emulator, that: bars stay transparent and edge-to-edge, status/navigation bar icons render with correct contrast in both light and dark theme, and no screen content (including screens with bottom navigation via `NavigationSuiteScaffold` and any dialogs/sheets) is obscured by system bars.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `app-theme`: adds a requirement that the app theme must not set deprecated edge-to-edge window attributes (status/navigation bar color, light status bar) and must rely on `enableEdgeToEdge()` plus `WindowInsetsControllerCompat` for system bar appearance.

## Impact

- `src/app/src/main/res/values/themes.xml`
- `src/app/src/main/res/values-night/themes.xml`
- No code changes expected in `MainActivity.kt` or `ui/theme/Theme.kt` (both already correct); verification only.
- No dependency, schema, or API changes.
