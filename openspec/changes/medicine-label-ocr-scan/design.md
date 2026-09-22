## Context

The Add medicine form (`medicine-add`) captures name, default dose (amount + unit), used since, use until, prescribed by and schedules, all typed by hand. This change adds an optional camera/gallery-driven shortcut that prefills name and dose from a photo of the medicine's label or box, using on-device OCR, without changing what the form saves or how it validates.

## Goals / Non-Goals

**Goals:**
- Prefill the name and, where recognisable, the dose amount/unit fields from a photo, entirely on-device.
- Keep the form's existing validation, review and save behaviour completely unchanged — OCR only ever changes what's *pre-typed*, never what's accepted.
- Degrade to exactly today's manual-entry experience when there's no camera, the permission is declined, or nothing is recognised.

**Non-Goals:**
- No drug database lookup, no interaction checking, no medical interpretation of recognised text.
- No cloud OCR or any network call.
- No persistence of the captured photo beyond the scan action.
- No attempt to recognise "used since", "use until" or "prescribed by" from the label — labels rarely carry this reliably, and guessing it risks a confidently-wrong prefill for fields that gate validation elsewhere.

## Decisions

- **OCR engine: ML Kit Text Recognition v2 (bundled/on-device model).** Chosen over building a custom recognizer (meaningful complexity ML Kit already solves — text-block geometry, script detection, rotation handling) and over a cloud OCR API (would violate the no-network-access rule outright). The bundled model variant is used specifically so recognition works with no model download over the network the first time the feature runs.
- **Photo lifecycle: capture (or pick) → recognise → discard.** The `Uri` handed back by the camera intent or photo picker is read once for `InputImage` construction and never written to app storage; any camera-intent temp file created via `FileProvider` is deleted immediately after recognition completes or fails, in a `finally`, so a crash mid-recognition can't leak a stray photo.
- **Parsing heuristics live in the domain layer as a pure function.** `ParseMedicineLabelText(rawText: String): LabelScanResult` takes the raw recognised text block and returns a candidate name and an optional candidate dose (amount + unit token), using simple line/regex heuristics (first non-empty line as name candidate; a `<number><space?><unit>` regex over the known unit list from `medicine-add` for dose). This keeps the parsing unit-testable with zero Android or ML Kit dependency, per CLAUDE.md's layering rule — only the camera call and the ML Kit `TextRecognizer` invocation live in the data/UI layer.
- **No auto-accept, ever.** `LabelScanResult` only prefills the existing form's mutable state; the user still taps into each field, sees exactly what was recognised, and can change or clear anything before Save. This matches how a typed value works today and needs no new validation path.
- **Permission handling: request camera only when "Scan label" is tapped, not on form open.** Declining the request, or the device lacking a camera entirely (`PackageManager.FEATURE_CAMERA_ANY` absent), simply hides the outcome of that tap behind a message and leaves the rest of the form exactly as usable as it is today. The photo-picker path (no CAMERA permission needed, `ActivityResultContracts.PickVisualMedia`) is offered as an alternative entry so a camera-less or permission-declined device can still use an existing photo if one already exists on the device.
- **Manifest declares the camera feature as `android:required="false"`.** So Pillsner keeps installing on devices without a camera; CLAUDE.md's minSdk rule is unaffected since ML Kit Text Recognition's minSdk (21) is already below Pillsner's floor.

## Risks / Trade-offs

- [Risk] ML Kit adds APK size (bundled model is several MB) → Mitigation: accepted as the named, justified trade-off in the proposal; no dynamic feature-module delivery is introduced since that would need Play Feature Delivery, out of scope here.
- [Risk] Heuristic parsing misreads a name/dose from a noisy label (e.g. picks a manufacturer name over the drug name) → Mitigation: this is fully expected and acceptable because nothing is auto-saved; the user reviews and corrects every field before Save, same as any typed entry today.
- [Risk] Users may expect dose *units not in the existing fixed list* (mg, g, mcg, ml, tablet, capsule, drop, puff, unit) to be recognised → Mitigation: the parser only proposes a unit when it matches that existing fixed list; anything else leaves the dose fields empty for manual entry rather than guessing.
- [Risk] Camera permission prompt interrupts an otherwise keyboard/TalkBack-only flow → Mitigation: "Scan label" is clearly optional and adjacent to, not in front of, the manual fields; the rest of the form's accessibility contract (spoken labels, error announcements, largest-font-scale scrolling) is untouched.

## Migration Plan

No data migration — this adds a UI action and a domain parsing function, no schema change. Ships as one change; if the dependency proves too costly (APK size, model quality) it can be reverted by removing the "Scan label" entry point and the ML Kit dependency without touching persisted data.

## Open Questions

None outstanding — the issue's open questions (OCR library choice, parsing aggressiveness, permission/accessibility handling) are resolved above.
