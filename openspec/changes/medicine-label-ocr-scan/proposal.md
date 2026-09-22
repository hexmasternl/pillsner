**GitHub Issue:** #33 (https://github.com/hexmasternl/pillsner/issues/33)

## Why

Adding a medicine today means typing every field by hand — name, dose amount, unit, dates, prescriber — which is real friction for someone managing several medications, and worse for older users or long medication names. On-device text recognition can remove the typing for name and dose without sending a single byte off the phone, so the "keep the data on the device" promise stays intact while the "add medicine" flow gets meaningfully faster.

## What Changes

- Add an optional **"Scan label"** action on the Add medicine form that opens the camera (or lets the user pick an existing photo) and runs on-device OCR (ML Kit Text Recognition v2 — bundled, on-device model, no network call once installed) against the image.
- Recognised text is parsed with simple heuristics into a candidate name and, when present, a candidate dose amount + unit; both prefill the existing form fields. Nothing is auto-saved and nothing is auto-recognised as final — the user reviews, corrects and confirms exactly as with manual entry.
- If recognition finds nothing usable, the form stays empty and the user types as they do today — this is purely an optional shortcut alongside manual entry, never a replacement for it.
- The captured/selected photo is discarded immediately after the form fields are prefilled; Pillsner never retains it, consistent with not holding image data that could carry incidental personal information beyond the moment it's needed.
- Adds a new runtime-permission touchpoint (camera) that must degrade gracefully: declining or lacking a camera leaves the form exactly as usable as it is today, including with TalkBack and at the largest font scale.
- Adds a new third-party dependency: `com.google.mlkit:text-recognition` (and its Latin-script model). Justification per CLAUDE.md's dependency bar: on-device OCR is nontrivial to build in-house, ML Kit's text recognizer runs fully on-device with no network call once its model is present, ships from Google as a maintained AndroidX-adjacent library, and removes meaningful complexity (image preprocessing, text-block geometry, script detection) that would otherwise have to be hand-rolled. This must be named in `README.md`'s dependency disclosure once the change ships, per CLAUDE.md's privacy section ("No network access, third-party SDKs... unless an accepted proposal adds them and the README discloses them").

## Capabilities

### New Capabilities

None — this extends the existing add-medicine capability rather than introducing a new domain concept.

### Modified Capabilities

- `medicine-add`: add a "Scan label" entry point on the Add medicine form, the on-device recognition-to-prefill behaviour, the review-before-save guarantee, the photo-not-retained guarantee, and the graceful no-camera / nothing-recognised fallback to manual entry.

## Impact

- **UI**: `medicine-add` screen gains a "Scan label" affordance, a camera/photo-picker launch, and a brief in-progress state while recognition runs; no new screen.
- **Domain**: a new label-parsing use case (image → candidate name/dose text) sits in the domain layer only as a pure text-parsing function; the camera capture and ML Kit call themselves are Android-framework/data-layer concerns, keeping the domain layer free of Android and ML Kit dependencies per CLAUDE.md's layering rule.
- **Dependencies**: adds `com.google.mlkit:text-recognition` to the version catalog; no other new dependency.
- **Permissions**: adds `android.permission.CAMERA` (runtime-requested, not declared as required — the manifest entry must not force camera availability, since the feature is optional and the app must install and fully function on camera-less devices).
- **Privacy**: no network access is introduced; the recognised text and the photo never leave the device, and the photo is not persisted anywhere, including caches, beyond the lifetime of the scan action.
- **README**: needs a line added to the disclosed dependencies once this change ships.
