## Context

The Add medicine form (`ui/medicines/form/`) is a flat draft of plain fields (`MedicationFormUiState`: `name: String`, `doseText: String`, `doseUnit: DoseUnit`, ...) driven by `MedicationFormViewModel`. Dependency injection is a single hand-written `AppContainer` (`di/AppContainer.kt`) with no framework. The codebase already has one precedent for a runtime-permission request flow, `ui/home/NotificationPermissionRequest.kt` (`rememberLauncherForActivityResult` + a `Context` helper to check current grant state), which this design follows for the new `CAMERA` permission.

This was explored via `/opsx:explore` (see [issue #33](https://github.com/hexmasternl/pillsner/issues/33) comments) before proposing; the bundled-vs-unbundled ML Kit trade-off below was the central finding from that session.

## Goals / Non-Goals

**Goals:**
- Recognize text from a photo of a medication label entirely on-device, with zero network access, and use it to prefill the Add medicine form's name, dose amount and unit.
- Keep manual entry fully functional and as the primary path; scanning is strictly optional and additive.
- Never retain the captured/selected photo beyond the moment the form is prefilled from it.
- Support all six of Pillsner's languages for the unit-word parsing step (recognition itself is language-agnostic within Latin script).

**Non-Goals:**
- No drug database lookup, no interaction checking, no dosage validation, no medical interpretation of recognized text.
- No support for non-Latin scripts (Pillsner's supported languages are all Latin-script; Chinese/Devanagari/Japanese/Korean ML Kit models are out of scope).
- No editing of an existing medicine via scan in this change — scope is limited to the Add flow (`MedicationFormMode.Add`); extending to edit mode is a candidate for a later change, not blocked by anything here.
- No change to the `medicine-add` spec's existing requirements (fields, validation, save semantics) — scanning only ever writes into the same draft fields a keystroke would.

## Decisions

### D1: Use the bundled ML Kit Text Recognition v2 Latin model, not the unbundled/Play-services-backed variant

`com.google.mlkit:text-recognition` (bundled) ships its ~4MB Latin-script model inside the APK and recognizes text with no network call, ever. The alternative, `com.google.android.gms:play-services-mlkit-text-recognition` (unbundled), is only ~260KB in the APK but downloads its model via Google Play services on first use, which needs network access and in practice an `INTERNET` permission declaration.

Pillsner's README states, as a permission-table headline, that the app "declares no internet permission at all." Adding `INTERNET` to add a typing shortcut would reverse a stated product promise for a ~3.7MB size saving. Bundled is the only option that keeps that promise, so it is the only option considered further.

**Alternative considered**: unbundled, gating the download behind a "your first scan may take a moment to set up" message. Rejected — it still requires declaring `INTERNET`, which changes the app's threat model and its public claims regardless of how the download is messaged.

### D2: One Latin-script model covers all six supported languages

ML Kit's Latin recognizer already covers English, Dutch, German, French, Spanish and Portuguese; Pillsner needs no additional per-language recognition model. The only per-language work is downstream of recognition: a small unit-word vocabulary (e.g. "mg", "tablet", "comprimé", "Tablette", "comprimido") used to spot a dose amount + unit in the recognized text. This vocabulary lives in the domain layer next to `DoseUnit`, keyed by the app's existing language setting (`LanguageRepository`), not by locale detection from the image itself — the label may be in a different language than the app is set to, and guessing at that is unnecessary complexity for a feature that always ends in user review.

### D3: Recognition happens in a small domain-adjacent service, not inline in the ViewModel

A `LabelTextRecognizer` (or similar) wraps the ML Kit `TextRecognizer` client, takes an image, and returns recognized text (or nothing). A separate, pure, unit-testable domain function (e.g. `ParseLabelText`) takes that raw text plus the current app language and returns a best-effort `LabelScanResult` (nullable name, nullable (amount, unit) pair). `MedicationFormViewModel` only ever sees the parsed result and applies it to the existing draft fields — it never touches ML Kit types directly. This keeps ML Kit at the edge of the system and the actual "what does this text mean" logic fully unit-testable without Android or Play services, consistent with CLAUDE.md's domain-layer rule.

```
Photo (camera or picker)
      │
      ▼
LabelTextRecognizer (ML Kit, on-device, Android-dependent)
      │  raw recognized text
      ▼
ParseLabelText (pure domain function, unit-tested)
      │  LabelScanResult(name?, amount?, unit?)
      ▼
MedicationFormViewModel.applyScanResult(result)
      │  writes into existing draft fields (name, doseText, doseUnit)
      ▼
MedicationFormUiState  ── user reviews/edits everything, taps Save as normal
```

### D4: Camera permission follows the existing `NotificationPermissionRequest` pattern

`CAMERA` is requested only when the user taps "Scan label," using `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`, mirroring the structure already established for `POST_NOTIFICATIONS`. A denial (or a device with no camera) falls back to the photo picker (`ActivityResultContracts.PickVisualMedia`, no permission needed on API 26+ targets via the system picker) or, failing that, simply leaves manual entry as the only path — the "Scan label" action itself is always optional chrome on top of the unchanged form.

### D5: The photo is held only in memory for the duration of recognition

The captured/picked image is decoded to a `Bitmap`/`InputImage` in memory, passed to the recognizer, and discarded once `MedicationFormViewModel` has applied (or failed to apply) a result. It is never written to app-controlled storage and never becomes part of the persisted `MedicationFormDraft`. If the system camera app saves a copy to MediaStore (standard capture-intent behavior), that copy is the user's own gallery photo, outside Pillsner's control, same as any other camera-using app — Pillsner itself creates no additional copy.

## Risks / Trade-offs

- **[Risk] ~4MB APK size increase** → Accepted deliberately (D1); it is the cost of keeping the no-network promise. Should be called out in release notes / CHANGELOG since it's a user-visible download-size change, not hidden.
- **[Risk] Recognized text is often wrong or partial (poor lighting, curved labels, handwriting on a pharmacy sticker)** → Mitigated by design, not by better OCR: prefill is always provisional, every field stays editable, and there is no "trust the scan" fast path that skips review. Empty/garbage recognition simply leaves the field as it was.
- **[Risk] Parsing a dose/unit out of free text is a much softer heuristic than the strict validation the form already enforces** → `ParseLabelText` only ever offers a value; the existing `MedicationFormUiState` validation (positive decimal, known unit) still runs the same as if the user had typed it, so a bad parse surfaces as the existing "fix this field" error, never a silent bad save.
- **[Risk] Per-language unit vocabulary drifts out of sync with the six supported languages over time** → Same shape of risk as the existing string-resource localization the project already carries across `values-*`; no new process needed, just discipline at review time.
- **[Trade-off] Scoped to Add mode only, not Edit** → Keeps the change smaller and avoids ambiguity about scanning overwriting a medicine that already has recorded doses tied to its name/amount (`medicine-add` spec's "editing never rewrites doses already answered" rule). Revisit as a follow-up if there's demand.

## Migration Plan

No data migration: this adds a new optional entry point and a new dependency, and does not touch Room schema, persisted models, or any existing behavior of the Add medicine form. Rollback is a plain revert (remove the dependency and the entry point); nothing about existing medicines, schedules or doses is affected either way.

## Open Questions

- Whether "Scan label" should also appear on the Edit medicine form in a later change, and if so, what it should be allowed to overwrite given doses already recorded under the current name/amount.
- Exact confidence bar for offering a prefilled amount/unit vs. leaving that field untouched and only prefilling the name — to be settled against real label photos during implementation rather than decided abstractly here.
