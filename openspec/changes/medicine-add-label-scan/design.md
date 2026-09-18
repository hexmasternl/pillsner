## Context

The Medicines screen (`ui/medicines/MedicinesScreen.kt`) shows one `FloatingActionButton` ("Add medicine") that navigates to `MedicationFormGraph`, a nested nav graph whose route already carries an optional argument (`medicationId: Long? = null`) distinguishing add mode from edit mode. The Add medicine form (`ui/medicines/form/`) is a flat draft of plain fields (`MedicationFormUiState`: `name: String`, `doseText: String`, `doseUnit: DoseUnit`, ...) driven by `MedicationFormViewModel`. Dependency injection is a single hand-written `AppContainer` (`di/AppContainer.kt`) with no framework. The codebase already has one precedent for a runtime-permission request flow, `ui/home/NotificationPermissionRequest.kt` (`rememberLauncherForActivityResult` + a `Context` helper to check current grant state), which this design follows for the new `CAMERA` permission.

This was explored via `/opsx:explore` (see [issue #33](https://github.com/hexmasternl/pillsner/issues/33) comments) before proposing; the bundled-vs-unbundled ML Kit trade-off below was the central finding from that session. The entry point was originally designed as a "Scan label" action inside the Add medicine form itself; this revision moves it to a camera button on the Medicines screen so it is visible before the user commits to typing anything, without changing any of the on-device-recognition reasoning.

## Goals / Non-Goals

**Goals:**
- Recognize text from a photo of a medication label entirely on-device, with zero network access, and use it to prefill a freshly opened Add medicine form's name, dose amount and unit.
- Make the shortcut discoverable from the Medicines screen, next to the button that already starts the manual Add flow.
- Keep manual entry fully functional and as the primary path; scanning is strictly optional and additive.
- Never retain the captured/selected photo beyond the moment the form is prefilled from it.
- Support all six of Pillsner's languages for the unit-word parsing step (recognition itself is language-agnostic within Latin script).

**Non-Goals:**
- No drug database lookup, no interaction checking, no dosage validation, no medical interpretation of recognized text.
- No support for non-Latin scripts (Pillsner's supported languages are all Latin-script; Chinese/Devanagari/Japanese/Korean ML Kit models are out of scope).
- No scan entry point on the Edit medicine form or anywhere in the edit flow — scope is limited to starting a brand-new medicine (`MedicationFormMode.Add`); extending to edit mode is a candidate for a later change, not blocked by anything here.
- No change to the `medicine-add` spec's existing requirements (fields, validation, save semantics) — scanning only ever supplies the same draft fields a keystroke would, before the user ever sees the form.

## Decisions

### D1: Use the bundled ML Kit Text Recognition v2 Latin model, not the unbundled/Play-services-backed variant

`com.google.mlkit:text-recognition` (bundled) ships its ~4MB Latin-script model inside the APK and recognizes text with no network call, ever. The alternative, `com.google.android.gms:play-services-mlkit-text-recognition` (unbundled), is only ~260KB in the APK but downloads its model via Google Play services on first use, which needs network access and in practice an `INTERNET` permission declaration.

Pillsner's README states, as a permission-table headline, that the app "declares no internet permission at all." Adding `INTERNET` to add a typing shortcut would reverse a stated product promise for a ~3.7MB size saving. Bundled is the only option that keeps that promise, so it is the only option considered further.

**Alternative considered**: unbundled, gating the download behind a "your first scan may take a moment to set up" message. Rejected — it still requires declaring `INTERNET`, which changes the app's threat model and its public claims regardless of how the download is messaged.

### D2: One Latin-script model covers all six supported languages

ML Kit's Latin recognizer already covers English, Dutch, German, French, Spanish and Portuguese; Pillsner needs no additional per-language recognition model. The only per-language work is downstream of recognition: a small unit-word vocabulary (e.g. "mg", "tablet", "comprimé", "Tablette", "comprimido") used to spot a dose amount + unit in the recognized text. This vocabulary lives in the domain layer next to `DoseUnit`, keyed by the app's existing language setting (`LanguageRepository`), not by locale detection from the image itself — the label may be in a different language than the app is set to, and guessing at that is unnecessary complexity for a feature that always ends in user review.

### D3: Capture and recognition happen before navigation; the result reaches the form as an optional nav-graph argument, the same way `medicationId` already does

The scan button lives on the Medicines screen, one level up from the form it feeds. Rather than opening the form first and letting it drive capture (the form-hosted design this revises), the Medicines screen's own composable runs the whole capture → recognize → parse sequence, then navigates into `MedicationFormGraph` exactly once with the result already in hand.

`MedicationFormGraph`'s route gains optional fields alongside its existing `medicationId`:

```kotlin
@Serializable
data class MedicationFormGraph(
    val medicationId: Long? = null,
    val scannedName: String? = null,
    val scannedDoseAmount: String? = null,
    val scannedDoseUnit: DoseUnit? = null,
)
```

This is the same mechanism the graph already uses to distinguish add mode from edit mode, so it needs no new state-holding class, no "hot potato" `SavedStateHandle` result pattern, and survives rotation and process death for free — Navigation-Compose restores route arguments from the saved back stack exactly as `medicationId` already is. `MedicationFormViewModel` reads these once, only when constructing a fresh add-mode draft (`medicationId == null`); when any are present it seeds `name`, `doseText` and `doseUnit` with them instead of the normal empty defaults, then proceeds exactly as if the user had typed those values. Edit mode ignores them entirely — they are never set when opening an existing medicine.

```
Medicines screen: user taps the scan button
      │
      ▼
Camera or photo picker (system intent, Android-dependent)
      │  in-memory image
      ▼
LabelTextRecognizer (ML Kit, on-device, Android-dependent)
      │  raw recognized text
      ▼
ParseLabelText (pure domain function, unit-tested)
      │  LabelScanResult(name?, amount?, unit?)
      ▼
navController.navigate(MedicationFormGraph(scannedName = ..., scannedDoseAmount = ..., scannedDoseUnit = ...))
      │
      ▼
MedicationFormViewModel (add mode only) seeds its initial draft from the route args
      │
      ▼
MedicationFormUiState  ── user reviews/edits everything, taps Save as normal
```

**Alternative considered**: keep the "Scan label" action inside the form and have the Medicines screen's button simply open the form first. Rejected — it reintroduces an extra tap and hides the shortcut exactly where the proposal wants it visible; the route-argument hand-off above costs nothing extra since the graph already threads an optional argument into the same view model.

### D4: Camera permission follows the existing `NotificationPermissionRequest` pattern, requested from the Medicines screen

`CAMERA` is requested only when the user taps the scan button, using `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`, mirroring the structure already established for `POST_NOTIFICATIONS`. A denial (or a device with no camera, checked via `PackageManager.FEATURE_CAMERA_ANY`) falls back to the photo picker (`ActivityResultContracts.PickVisualMedia`, no permission needed on API 26+ targets via the system picker); if that is also unavailable or the user cancels either path, the Medicines screen simply remains as it was — the button is always optional chrome alongside the unchanged add flow.

### D5: The photo is held only long enough to recognize it, then discarded

Capturing via the system camera intent needs a destination the camera app can write to, so a temporary file is created through a `FileProvider` in the app's own cache directory, decoded to a `Bitmap`/`InputImage`, passed to the recognizer, and deleted immediately afterwards — on success, on "nothing recognized," and on any error alike. Picking an existing photo reads it directly into memory with no copy written at all. Neither path ever becomes part of the persisted `MedicationFormDraft`, and no photo is ever attached to a saved medicine.

## Risks / Trade-offs

- **[Risk] ~4MB APK size increase** → Accepted deliberately (D1); it is the cost of keeping the no-network promise. Should be called out in release notes / CHANGELOG since it's a user-visible download-size change, not hidden.
- **[Risk] Recognized text is often wrong or partial (poor lighting, curved labels, handwriting on a pharmacy sticker)** → Mitigated by design, not by better OCR: prefill is always provisional, every field stays editable, and there is no "trust the scan" fast path that skips review. Nothing usable recognized simply opens the form at its normal empty defaults.
- **[Risk] Parsing a dose/unit out of free text is a much softer heuristic than the strict validation the form already enforces** → `ParseLabelText` only ever offers a value; the existing `MedicationFormUiState` validation (positive decimal, known unit) still runs the same as if the user had typed it, so a bad parse surfaces as the existing "fix this field" error, never a silent bad save.
- **[Risk] Per-language unit vocabulary drifts out of sync with the six supported languages over time** → Same shape of risk as the existing string-resource localization the project already carries across `values-*`; no new process needed, just discipline at review time.
- **[Trade-off] Scoped to starting a new medicine only, not editing one** → Keeps the change smaller and avoids ambiguity about scanning overwriting a medicine that already has recorded doses tied to its name/amount (`medicine-add` spec's editing behavior). Revisit as a follow-up if there's demand.
- **[Risk] A temporary capture file, even briefly in app cache, is one more thing that must reliably be deleted** → Mitigated by deleting it in a `finally`-equivalent path that runs on every outcome (success, nothing recognized, or an exception), and by never registering it with `MediaStore`, so it is never visible outside the app's own cache.

## Migration Plan

No data migration: this adds a new optional entry point and a new dependency, and does not touch Room schema, persisted models, or any existing behavior of the Medicines screen or the Add medicine form beyond the new optional route arguments. Rollback is a plain revert (remove the dependency, the button and the route arguments); nothing about existing medicines, schedules or doses is affected either way.

## Open Questions

- Whether a scan entry point should also reach the Edit medicine form in a later change, and if so, what it should be allowed to overwrite given doses already recorded under the current name/amount.
- Exact confidence bar for offering a prefilled amount/unit vs. leaving that field untouched and only prefilling the name — to be settled against real label photos during implementation rather than decided abstractly here.
