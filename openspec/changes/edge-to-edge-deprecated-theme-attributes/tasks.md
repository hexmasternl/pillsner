## 1. Remove deprecated edge-to-edge theme attributes

- [x] 1.1 Remove `android:statusBarColor`, `android:navigationBarColor` and `android:windowLightStatusBar` from `Theme.Pillsner` in `src/app/src/main/res/values/themes.xml`, keeping `android:windowBackground`.
- [x] 1.2 Remove `android:statusBarColor`, `android:navigationBarColor` and `android:windowLightStatusBar` from `Theme.Pillsner` in `src/app/src/main/res/values-night/themes.xml`, keeping `android:windowBackground`.
- [x] 1.3 Confirm no other theme, style or manifest entry in `src/app` sets any of these deprecated attributes (`grep` across `src/app/src/main/res`).

## 2. Verification

- [x] 2.1 Run the full unit test suite (`./gradlew testDebugUnitTest` from `src/`) and confirm it passes.
- [x] 2.2 Run lint (`./gradlew lintDebug` from `src/`) and confirm it is clean, with no new edge-to-edge lint warnings.
- [ ] 2.3 Manually verify on a device or emulator running Android 15+ (API 35+): status and navigation bars stay transparent, bar icon contrast is correct in both Light and Dark theme choices, and no screen content (including the bottom-navigation screens and any dialogs/sheets) is obscured by the system bars.
- [ ] 2.4 Manually verify the same on a device or emulator running an older Android version (down to `minSdk` 26) to confirm edge-to-edge behaviour is unchanged below API 35.
- [ ] 2.5 Confirm in the Play Console pre-launch report (next upload) that both the "Edge-to-edge may not display for all users" and "deprecated APIs or parameters for edge-to-edge" warnings are gone.
