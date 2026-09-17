## Why

Adding a medicine today means typing everything by hand — name, dose amount and unit — which is real friction for anyone managing several medications, especially with a long or unfamiliar drug name. Filed as [issue #33](https://github.com/hexmasternl/pillsner/issues/33) and refined through `/opsx:explore`: an on-device text recognizer can remove that typing without sending anything off the device, if it uses the bundled (not Play-services-downloaded) ML Kit model, which keeps the "no internet permission at all" promise intact at the cost of roughly 4MB of APK size. Pillsner's six supported languages are all Latin-script, so one bundled model covers recognition for all of them; only the smaller step of turning recognized text into a name/dose/unit guess needs per-language unit-word handling.

## What Changes

- Add a "Scan label" entry point on the Add medicine form that opens the camera or an existing photo, recognizes text on-device with the bundled ML Kit Text Recognition v2 Latin model, and prefills the form's name field and, when a recognizable amount + unit is found, the dose amount and unit fields.
- The user always reviews and can correct every prefilled field before saving; nothing is auto-saved from a scan.
- The captured/selected photo is discarded once the form has been prefilled from it; it is never persisted or retained.
- Manual entry remains the primary, always-available path: scanning is purely an optional shortcut, and the form behaves exactly as it does today if the user skips it, denies camera access, or nothing usable is recognized.
- No drug database, no interaction checking, no dosage validation and no medical interpretation of recognized text — this is a typing shortcut, not clinical guidance, consistent with Pillsner's disclaimer that it is not a source of medical advice.
- Adds the `com.google.mlkit:text-recognition` bundled dependency (Latin script only). Deliberately excludes the unbundled/Play-services-backed variant, since that requires a network-fetched model and would need an `INTERNET` permission the app does not otherwise declare.

## Capabilities

### New Capabilities
- `medicine-label-scan`: on-device photo capture, text recognition and best-effort prefill of the Add medicine form's name, dose amount and unit, with mandatory user review before save and no retention of the captured image.

### Modified Capabilities
(none — the Add medicine form's own fields, validation and save behavior, specified in `medicine-add`, are unchanged; scanning only supplies values into the same existing draft.)

## Impact

- **UI**: `ui/medicines/form/MedicationFormScreen.kt` and related composables gain a "Scan label" action; a new capture/review composable is added.
- **ViewModel/state**: `MedicationFormViewModel.kt` / `MedicationFormUiState.kt` gain a way to accept a recognized-text result and apply it to the existing draft fields (`name`, `doseText`, `doseUnit`).
- **Domain**: a new, unit-tested parser maps recognized text to a name guess and an optional (amount, unit) guess, with a small per-language unit-word vocabulary for the six supported languages.
- **DI**: `di/AppContainer.kt` gains a small text-recognition service wired the same way as existing services.
- **Dependencies**: adds `com.google.mlkit:text-recognition` (bundled Latin model, ~4MB) to the version catalog. No new permissions beyond `CAMERA` (already a common, well-understood runtime permission; requested only when the user taps "Scan label").
- **Permissions/manifest**: no change to `INTERNET`-related declarations; README's permission table gains one row for `CAMERA`, requested only for this optional feature.
- **Localization**: string resources for the new UI, plus the parser's unit-word vocabulary, extend across the existing `values/`, `values-de/`, `values-es/`, `values-fr/`, `values-nl/`, `values-pt/` resource sets.
