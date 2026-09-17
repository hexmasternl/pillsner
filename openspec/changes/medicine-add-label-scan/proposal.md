## Why

**GitHub Issue:** #33 (https://github.com/hexmasternl/pillsner/issues/33)

Adding a medicine today means typing everything by hand — name, dose amount and unit — which is real friction for anyone managing several medications, especially with a long or unfamiliar drug name. Filed as [issue #33](https://github.com/hexmasternl/pillsner/issues/33) and refined through `/opsx:explore`: an on-device text recognizer can remove that typing without sending anything off the device, if it uses the bundled (not Play-services-downloaded) ML Kit model, which keeps the "no internet permission at all" promise intact at the cost of roughly 4MB of APK size. Pillsner's six supported languages are all Latin-script, so one bundled model covers recognition for all of them; only the smaller step of turning recognized text into a name/dose/unit guess needs per-language unit-word handling.

Putting the entry point on the Medicines screen itself, rather than inside the Add medicine form, makes it a visible, one-tap shortcut the moment the user wants to add something — no need to open the form first to discover it exists.

## What Changes

- Add a camera icon button to the Medicines screen, next to the existing add (FAB) button, with the content description "Scan medicine label".
- Tapping it requests camera permission if not already decided, then opens the system camera to photograph a medicine label. If the device has no camera, or permission is denied, it falls back to picking an existing photo instead, so the shortcut still works.
- The captured or picked photo is recognized entirely on-device with the bundled ML Kit Text Recognition v2 Latin model. The photo is discarded immediately after recognition; it is never persisted or attached to the medicine record.
- The user is then taken straight to the Add medicine form (add mode, a fresh medicine — never editing an existing one), with the name field, and, where a recognizable amount and unit were found, the default dose amount and unit fields, prefilled from what was recognized.
- Every prefilled field remains an ordinary, editable draft value: the user always reviews and can correct it before saving, and nothing is auto-saved from a scan. If nothing usable is recognized, the form still opens with its normal empty defaults and a brief message says the photo could not be read.
- Manual entry remains fully available and unaffected: the regular add button still opens a blank form exactly as it does today, and scanning is purely an optional shortcut alongside it.
- No drug database, no interaction checking, no dosage validation and no medical interpretation of recognized text — this is a typing shortcut, not clinical guidance, consistent with Pillsner's disclaimer that it is not a source of medical advice.
- Adds the `com.google.mlkit:text-recognition` bundled dependency (Latin script only). Deliberately excludes the unbundled/Play-services-backed variant, since that requires a network-fetched model and would need an `INTERNET` permission the app does not otherwise declare.

## Capabilities

### New Capabilities
- `medicine-label-scan`: the camera button on the Medicines screen, on-device photo capture, text recognition, and handing a best-effort name/dose/unit draft to a freshly opened Add medicine form, with mandatory user review before save and no retention of the captured image.

### Modified Capabilities
- `medicine-overview`: the Medicines screen gains a second button, for scanning a label, alongside the existing add button.

## Impact

- **UI**: `ui/medicines/MedicinesScreen.kt` gains a scan button next to the add FAB; the capture/recognition flow it starts has no screen of its own beyond the system camera/photo-picker intents and a brief in-progress state.
- **Navigation**: the route that opens the Add medicine form in add mode gains optional scanned-value parameters, carried the same way its existing optional `medicationId` (edit mode) parameter is.
- **ViewModel/state**: `MedicationFormViewModel.kt` gains a way to seed a freshly created (add-mode) draft's `name`, `doseText` and `doseUnit` from those optional parameters; edit mode is never affected.
- **Domain**: a new, unit-tested parser maps recognized text to a name guess and an optional (amount, unit) guess, with a small per-language unit-word vocabulary for the six supported languages.
- **DI**: `di/AppContainer.kt` gains a small text-recognition service wired the same way as existing services.
- **Dependencies**: adds `com.google.mlkit:text-recognition` (bundled Latin model, ~4MB) to the version catalog. No new permissions beyond `CAMERA` (already a common, well-understood runtime permission; requested only when the user taps the scan button).
- **Permissions/manifest**: no change to `INTERNET`-related declarations; a `FileProvider` entry is added so the system camera intent has somewhere to write the temporary capture; README's permission table gains one row for `CAMERA`, requested only for this optional feature.
- **Localization**: string resources for the new button and messages, plus the parser's unit-word vocabulary, extend across the existing `values/`, `values-de/`, `values-es/`, `values-fr/`, `values-nl/`, `values-pt/` resource sets.
