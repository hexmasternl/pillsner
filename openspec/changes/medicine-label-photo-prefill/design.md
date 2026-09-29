## Context

The Add medicine form (`ui/medicines/form/`) is a flat draft (`MedicationFormDraft`: name, dose text and unit, used since, use until, prescriber, schedules) owned by `MedicationFormViewModel`, shared between the form and the schedule editor through the flow's navigation graph entry, and persisted through `SavedStateHandle` so it survives rotation and process death. Schedules are the three domain shapes in `Schedule` (every N days at clock times, on weekdays at clock times, every N hours from a first dose). Dependency injection is the hand-written `AppContainer`. The manifest deliberately declares no `INTERNET` permission, and the README's permission table says so as a headline. The one runtime-permission flow in the app today is `ui/home/NotificationPermissionRequest.kt`.

### What happened the first time

The archived change `medicine-add-label-scan` (issue #33, PR #38, shipped as 1.1.0) used `com.google.mlkit:text-recognition` with its bundled Latin model. That artifact transitively pulled in Firebase installations and Google data-transport components. Their own manifests declared `INTERNET` and `ACCESS_NETWORK_STATE` and registered content providers, and manifest merging added all of it to the shipped app without anyone changing Pillsner's own manifest. The result was a package that was not accepted as safe to install, and that on devices where it did install crashed at launch inside Google Play's integrity wrapper (`com.pairip.application.Application`, a `NullPointerException`). PR #46 stripped the two permissions with `tools:node="remove"`; the crash remained. PR #47 reverted `main` to the 1.0.2 tree. The exact mechanism of the crash was never isolated, which is precisely why this design does not try to patch around it: it removes the entire class of cause.

Two lessons shape everything below:

1. A dependency is not "offline" or "safe" because its documentation says so. What matters is what it contributes to the merged manifest and the dependency graph, and that has to be checked by the build, not by reading.
2. Every permission the app declares must be one it chose, named in one place, and explained to the user. This change adds exactly one, `CAMERA`, and the build fails if any other appears.

### Why live capture rather than a hand-off to the camera app

An earlier draft of this design used the system camera app so that no permission at all was needed. The user chose the in-app live-capture design instead, accepting the `CAMERA` permission, because it gives a framing guide, no shutter press, no app switch and no confirm screen, and because recognition can run on frames continuously and stop the moment it reads well. That decision is final for this change; the hand-off remains documented under D2 as the alternative.

## Goals / Non-Goals

**Goals:**

- Read a medicine label entirely on the device, with no model download, no network permission and no Google Play services, Firebase or ML Kit surface in the app.
- Live capture: the user holds the label in a frame and the app reads it as soon as it can, with a manual shutter as the fallback.
- Pre-fill name, default dose, one or more schedules, used since and use until from the recognised text, then hand the user the ordinary form to review and correct.
- Add exactly one permission, `CAMERA`, requested in context with a rationale. Make the complete list of manifest and dependency changes explicit in this document, in the README, and in a build check that fails when reality drifts from it.
- Never write a frame or photo to disk, and never log the recognised text.
- Keep the interpretation logic pure Kotlin, unit-tested against labels in all six app languages.
- Degrade to today's manual form, plus "Choose a photo", when the permission is declined, the device has no camera, the user cancels, or nothing usable is recognised.

**Non-Goals:**

- No drug database, no interaction or dosage checking, no medical interpretation beyond reading what is printed.
- No still-photo file, no `ImageCapture` use case, no FileProvider. Frames are analysed in memory only.
- No scan in edit mode; an existing medicine with recorded doses is not overwritten from a scan.
- No stock quantity, expiry date or prescriber pre-fill. The stock feature has its own form, and prescriber is a five-value choice the user picks in one tap.
- No non-Latin scripts. All six app languages are Latin-script.
- No dynamic delivery of models or native libraries. Everything ships in the base install so the feature works offline from the first launch.
- No CameraX extensions, no `camera-mlkit-vision`, no `camera-video`.

## Decisions

### D1: Tesseract 5 through Tesseract4Android, not ML Kit, not a cloud API, not a hand-rolled model pipeline

The recogniser is Tesseract OCR 5 (LSTM engine) with Leptonica, packaged for Android by [Tesseract4Android](https://github.com/adaptech-cz/Tesseract4Android) (`cz.adaptech.tesseract4android:tesseract4android-openmp`, Apache 2.0). It is a single AAR of native code plus a thin Java API. Its only declared Java dependency is `androidx.annotation`, it registers no components in its manifest, and it has no relationship with Google Play services, Firebase or ML Kit. The OpenMP flavour is chosen because Pillsner runs exactly one recogniser instance at a time and wants it to use every core, which matters more now that it runs on a stream of frames.

Why not the alternatives:

- **ML Kit (bundled or unbundled)**: this is the library that caused the 1.1.0 failure. Both flavours bring Firebase and data-transport transitively. Rejected outright. The same goes for CameraX's `camera-mlkit-vision` bridge, which the guard forbids by name.
- **A TensorFlow Lite / LiteRT or ONNX Runtime pipeline with an open OCR model (for example PaddleOCR)**: better accuracy on scene text and available from Maven Central, but it needs a detection model, a recognition model, custom post-processing of the detector's probability map (connected components, box merging, cropping, CTC decoding) and a dictionary file, all written and maintained in this repository. That is a lot of clever code in an app whose rule is "someone reading a bug at 2 a.m. should be able to follow it". Kept as the documented fallback if Tesseract's accuracy on real labels proves unacceptable (task 8.4 measures it).
- **Cloud OCR**: needs `INTERNET`. Rejected outright.
- **`tess-two`**: unmaintained since 2019, Tesseract 3. Rejected.

**Source repository.** Tesseract4Android is published through JitPack, not Maven Central. Pillsner's `settings.gradle.kts` uses `FAIL_ON_PROJECT_REPOS`, so JitPack is added there, with a content filter that admits only the group `cz.adaptech.tesseract4android`, so nothing else can ever resolve from it. Because JitPack builds from a tagged GitHub source, the SHA-256 of the resolved AAR is pinned in the guard task (D8) and the build fails if it changes. The version is pinned in the version catalog like every other dependency.

**Trained data.** Tesseract reads `.traineddata` files from a directory on disk. Pillsner bundles `eng.traineddata` from the `tessdata_fast` set under `app/src/main/assets/tessdata/` (taken at apply time from `tesseract-ocr/tessdata_fast` commit `923915d4ced2a7235221788285785a29c4a42d4a` of 14 September 2017, the only revision of that file in the repository; 4,113,088 bytes; SHA-256 `7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2`) and copies it to `filesDir/ocr/tessdata/` before the first scan (a version marker file next to it makes an app update replace it). That directory is excluded from cloud backup and device transfer in both backup rule files; it is derived data that any device can recreate from the app's own assets.

Why English only, at first: the parts of a label that matter (medicine name, numbers, units, dates) are language-neutral, and the vocabulary in D4 matches unit and frequency words accent- and case-insensitively, so "comprimé" read as "comprime" still matches. The per-language `tessdata_fast` files for the other five app languages together add about 13 MB (nld 6.0, spa 2.3, por 2.0, deu 1.5, fra 1.1). Task 8.4 measures recognition on real labels in each language; if a language performs badly, adding its file is a data change and a one-line list in the recogniser, not a design change. The `Latin` script model (89 MB) is out of the question.

### D2: An in-app live viewfinder on CameraX, with the `CAMERA` permission requested in context

The scanning screen is a destination inside the medication form's navigation graph (`LabelScan`), so it shares `MedicationFormViewModel` through the graph entry exactly as the schedule editor does, and hands its result straight into the shared draft. The screen has its own small `LabelScanViewModel` for camera state.

**Camera stack.** CameraX 1.6.2 (newest stable at proposal time; verify at apply): `androidx.camera:camera-core`, `camera-camera2`, `camera-lifecycle` and `camera-compose`. The last provides the `CameraXViewfinder` composable, so the preview is plain Compose with no `AndroidView` bridge. Two use cases are bound to the screen's lifecycle: `Preview` and `ImageAnalysis`. There is deliberately no `ImageCapture` use case: nothing is ever encoded to JPEG or written anywhere.

**Permission flow**, following the notification-permission precedent:

1. Tapping "Scan a label" on the form opens a sheet with "Scan with camera" and "Choose a photo". "Scan with camera" is offered only when `PackageManager.hasSystemFeature(FEATURE_CAMERA_ANY)` is true.
2. On "Scan with camera", if `CAMERA` is not granted, the app first shows its own rationale ("Pillsner uses the camera only to read the label in front of it. Nothing is saved or sent anywhere.") with "Continue" and "Not now", then launches `ActivityResultContracts.RequestPermission`.
3. Granted: navigate to `LabelScan`. Denied: stay on the form, show a snackbar saying the camera is unavailable and that a photo can still be chosen. Permanently denied (`shouldShowRequestPermissionRationale` false after a denial): the snackbar's action opens the app's system settings page.
4. The permission is never requested at install, at app start, or on opening the form.

**Manifest.** `<uses-permission android:name="android.permission.CAMERA"/>` and `<uses-feature android:name="android.hardware.camera.any" android:required="false"/>`, without `tools:replace`: task 2.4 read the merged manifests and no CameraX artifact declares the feature, and the merger warns on every build when `tools:replace` has nothing to replace. If a future CameraX version adds the feature as required, the guard will not catch it (it checks permissions, not features), so the merged manifest is re-read in the change that bumps CameraX.

**Alternative considered: hand the capture to the phone's camera app.** Needs no permission, no CameraX and no viewfinder, at the cost of an app switch, a confirm screen, a full-size JPEG on disk for the duration of the scan and no framing guidance. It was the first draft of this design and was set aside by the user's explicit decision in favour of live capture.

### D3: Frames are analysed continuously in memory; the scan accepts itself when two frames agree

**Frame path.** `ImageAnalysis` is configured with `OUTPUT_IMAGE_FORMAT_YUV_420_888`, `STRATEGY_KEEP_ONLY_LATEST`, a resolution selector preferring 1280 × 960, and a single-thread executor. For each frame, `LabelFrameRecogniser` (data layer):

1. Takes the Y plane only. It is already 8-bit greyscale, so there is no colour conversion at all.
2. Crops to the framing guide's rectangle mapped into frame coordinates and rotates by `imageInfo.rotationDegrees` so the text is upright, both as plain byte-array loops.
3. Hands the bytes to one long-lived `TessBaseAPI` (initialised once when the screen opens, released when it closes) through `setImage(bytes, width, height, bytesPerPixel = 1, bytesPerLine = width)` with `PSM_AUTO`, and reads the result at text-line level into `RecognisedLine(text, confidence)` in reading order. `PSM_SPARSE_TEXT` is the documented switch to try in task 8.4 if labels with scattered text read badly.
4. Closes the `ImageProxy` in a `finally`, so the next frame can arrive. Frames that arrive while recognition is busy are dropped by CameraX; the screen never queues them.

Recognition of one cropped frame is expected to take a few hundred milliseconds on a mid-range phone with the fast model and OpenMP, giving one to three reads per second. The preview itself runs at full frame rate regardless, because analysis is on its own thread.

**Acceptance rule.** Each frame's lines go through `InterpretLabelText` (D4). A frame is *good* when its interpretation has a name and at least one of a default dose or a schedule. The scan accepts itself when two consecutive good frames yield the same name (compared case- and whitespace-insensitively); the later frame's interpretation is used. On acceptance the screen vibrates briefly, announces "Label read", and pops back to the form with the interpretation. Two frames rather than one keeps a single misread from being accepted; more than two makes the user hold still for noticeably longer for little gain.

**Shutter.** A shutter button takes the most recent frame's interpretation, good or not, and returns it. When no frame has produced anything yet, it returns an empty interpretation and the form shows "nothing readable" (D5). This is also how a TalkBack user, who cannot see the framing guide, completes a scan on their own terms.

**Guidance.** After 8 seconds without a good frame the hint text changes to "Move closer, add light or hold still"; after 20 seconds the shutter button gains emphasis. A torch toggle is offered when the camera reports a flash unit. Cancel returns to the form unchanged.

**Picked photo path.** "Choose a photo" uses `ActivityResultContracts.PickVisualMedia` (no permission), reads the picked file through `ContentResolver.openInputStream` exactly once into memory, decodes bounds, EXIF orientation (all eight values) and pixels from those bytes with `inSampleSize` bounding the long side to 2,000 pixels, converts to greyscale bytes, and runs the same recogniser once on the whole image. It is never copied to storage.

Nothing here logs frames, text or confidence. The only log lines are debug-level "scan opened", "accepted after N frames in N ms", "cancelled" and failure classes without payload.

### D4: Interpretation is a pure domain function with a multilingual vocabulary

`InterpretLabelText(lines: List<RecognisedLine>, today: LocalDate): LabelInterpretation` lives in `domain/labelscan/`, imports nothing from Android, CameraX or Tesseract, and is where all the rules live. It is deterministic, cheap enough to run on every frame, and unit-tested per rule and per language.

`LabelInterpretation` holds: `name: String?`, `defaultDose: Quantity?`, `schedules: List<Schedule>`, `usedSince: LocalDate`, `useUntil: LocalDate?`, `rawText: String`. Absent means "leave the field as it is". `isEmpty` is true when name, default dose and schedules are all absent.

**Vocabulary.** One table, not keyed by the app language, because the label was printed by the pharmacy in its language, not the phone's. Matching is case-insensitive and accent-insensitive (Unicode NFD, marks stripped). The table maps words in English, Dutch, German, French, Spanish and Portuguese to:

- **Units** (`DoseUnit`): mg/milligram; g/gram; mcg/µg/ug/microgram; ml/mL/millilitre; tablet, tabletten, Tablette(n), comprimé(s), comprimido(s), tab(s); capsule(s), kapsel(n), gélule(s), cápsula(s); drop(s), druppel(s), Tropfen, goutte(s), gota(s); puff(s), pufje(s), inhalatie(s), Hub, Sprühstoß, bouffée(s), inhalación, inalação; unit(s), eenheid/eenheden, Einheit(en), unité(s), unidad(es), IE, IU, UI.
- **Frequency words**: "per day" forms (daags, per dag, maal daags, times a day, daily, täglich, mal täglich, fois par jour, veces al día, vezes ao dia), "every N hours" forms (om de N uur, every N hours, alle N Stunden, toutes les N heures, cada N horas, a cada N horas), "every other day" forms (om de dag, every other day, jeden zweiten Tag, tous les deux jours, cada dos días, dia sim dia não), day-part words (morning/noon/evening/night in all six languages), and the box notation `1-0-1`, `1-1-1`, `1-0-0-1` (morning, noon, evening, optional night).
- **Duration words**: day, week (and plurals) in all six languages, with "for/during" forms (gedurende, for, lang, pendant, durante), and "until" forms (tot, t/m, until, bis, jusqu'au, hasta, até).
- **Noise lines** to skip when choosing the name: pharmacy words (apotheek, pharmacy, Apotheke, pharmacie, farmacia, farmácia), salutations (dhr, mevr, Mr, Mrs, Ms, Herr, Frau, M., Mme, Sr., Sra.), phone-number and postcode shapes, lines that are only a date, and lines that match a frequency pattern.

**Rules, in order:**

1. **Dose tokens.** Every `<number><unit>` match, where the number is an integer or a decimal with `.` or `,` and the unit is a vocabulary word, becomes a `Quantity`. Strength tokens are those in a mass or volume unit (mg, g, mcg, ml). Count tokens are those in a form unit (tablet, capsule, drop, puff, unit).
2. **Default dose** is the first strength token on the label. When there is none, it is the first count token that sits in an instruction line (a line that matches a frequency pattern), such as "1 tablet" in "take 1 tablet twice a day". When there is neither, the default dose is absent.
3. **Schedule amount** is the count token in the instruction line when there is one; otherwise a strength or volume token on that same line ("Take 10 ml twice daily" on a "125 mg/5 ml" label is 10 ml, not 125 mg); otherwise the default dose. A schedule is produced only when both an amount and a frequency were found.
4. **Frequency to schedule**, using the app's existing shapes:
   - N times a day, for N from 1 to 4, becomes `EveryNDays(interval 1)` with fixed default times: 1 → 08:00; 2 → 08:00, 20:00; 3 → 08:00, 14:00, 20:00; 4 → 08:00, 12:00, 16:00, 20:00. "once" and "twice" words count as 1 and 2.
   - N times a day for N above 4 produces no schedule, as the spec requires: an every-N-hours shape from 08:00 would not even yield N doses in a day, so there is no honest mapping (corrected at apply time; an earlier draft mapped it to `EveryNHours(24 / N)`).
   - Every N hours becomes `EveryNHours(N, from 08:00)` when N is one of those intervals; otherwise no schedule.
   - Every other day becomes `EveryNDays(interval 2, 08:00)`.
   - Day-part words become `EveryNDays(interval 1)` with the matching slots: morning 08:00, noon 13:00, evening 18:00, night 22:00.
   - Box notation `a-b-c[-d]` maps the non-zero slots to the same four slot times. Each digit is a number of doses: with a count unit (from a count token on an instruction line, or a bare form word on the box line) the amount is that many of the unit; without one it is the digit times the default dose, so `1-0-1` on a 50 mg label is 50 mg morning and evening and `2-0-2` is 100 mg. When every non-zero digit is the same, that is one schedule. When the digits differ, one schedule is produced per distinct non-zero digit, each with its own times.
   - The weekdays shape is never produced; labels do not carry it in a form worth guessing.
   All produced schedules are validated by the same domain constructors the editor uses; anything they reject is dropped rather than patched.
5. **Name.** Candidate lines are all lines that are not noise lines, with every dose token, pack-size token ("30 tabletten", "N st", "N pcs") and trailing punctuation removed, whitespace collapsed, and at least three letters left. The name is the first candidate that contains a strength token in the original line (name and strength share a line on most labels), otherwise the first candidate. The result is truncated to 60 characters, the form's name field length. Case is kept as recognised.
6. **Dates.** Recognised in the numeric forms `dd-mm-yyyy`, `dd/mm/yyyy`, `dd.mm.yyyy`, `yyyy-mm-dd` and the same with a two-digit year. A month-and-year only form (`03/2028`) is never a date candidate: that is how expiry dates are printed. Used since is the most recent candidate that is on or before today and not more than 365 days before it; when there is none, it is today. Any candidate after today is treated as an expiry or "until" date, never as a start.
7. **Use until.** In priority order: a date candidate that follows an "until" word and is after used since; otherwise a duration "N days" or "N weeks" after a "for/during" word or standing alone in an instruction line, giving used since plus N days minus one (a 7-day course that starts today ends on the seventh day, inclusive) or plus 7·N days minus one for weeks; otherwise absent. A result before used since is discarded.
8. **Raw text** is the recognised lines joined with newlines, for the "Show recognised text" sheet only.

Every rule is small and every rule has its own test, including fixtures for a Dutch pharmacy label, a German box with `1-0-1`, a French "1 comprimé matin et soir pendant 7 jours", a Spanish "cada 8 horas", a Portuguese "2 vezes ao dia durante 2 semanas" and an English "take one tablet twice daily for 10 days". Fixtures use invented medicine names and are not real people's labels.

### D5: The scan starts on the form and lands in the shared draft

"Scan a label" is the trailing icon of the Name field on the Add medicine form, and only in `MedicationFormMode.Add`: a camera `IconButton` (48 dp target, spoken label "Scan a label") rather than a separate button, so the form gains no extra row and the action sits where the typing it replaces would start (changed at apply time from an outlined button above the field, at the user's request). Tapping it opens a bottom sheet (8.12) with "Scan with camera" and "Choose a photo", the first hidden on a device without a camera.

The scanning screen (D2, D3) returns a `LabelInterpretation` to the shared `MedicationFormViewModel` by calling `onInterpretationReceived` before popping itself. The picked-photo path runs the recogniser in `viewModelScope` with `isScanning = true` and a modal "Reading the photo…" state with Cancel, then calls the same method.

Applying the result:

- When the draft is untouched (equal to its initial state), the interpretation is applied at once.
- When the draft already has edits, the form asks "Replace what you have entered with what was read from the label?" with Keep and Replace. Replace applies the interpretation; Keep discards it.
- Applying sets exactly the fields the interpretation carries: name; dose text (formatted with the form's `AmountParser`) and unit; schedules (replacing the list); used since; use until. Absent fields are left as they are. Prescriber and active flag are never touched. The draft is persisted through `DraftSaver` as after any edit, so the pre-fill survives rotation.
- The form then shows an attention banner (8.9) at the top: "Filled in from your label scan. Check every field before saving." with two actions, "Show text" and dismiss. Built like the design system's attention banner but on `secondaryContainer` with an info icon, not `errorContainer`: red is reserved for danger (design system 2.4), and a pre-filled form is something to check, not a failure (apply-time clarification). "Show text" opens a sheet with the raw recognised text in body type, selectable, nothing else. The banner stays until dismissed or until the form closes; it is not shown again on rotation once dismissed (a flag in the saved state). The recognised text itself is a plain view-model field, never saved state: it can carry a patient's name or address, and the privacy statement promises it exists only in memory while the form is open. After process death the banner therefore returns without its "Show text" action (apply-time correction, review on #86).
- When the interpretation is empty, the form is not changed and a snackbar says nothing readable was found. A decoding or recogniser failure gives a different snackbar ("The photo could not be read"). Neither names the medicine or quotes text.

Pre-filled values go through the ordinary validation on Save; there is no separate path, so a bad read surfaces as the existing field error.

**Alternative considered: a camera button on the Medicines screen that opens the form already filled (the entry point of the first attempt).** It saves one tap but needs route arguments for every field including a list of schedules, cannot use the shared draft, and gives the user no way to scan again from the form. Starting on the form keeps one code path and the legal-acceptance gating the form already has.

### D6: Nothing is retained

- Camera frames exist only as `ImageProxy` buffers owned by CameraX and the cropped greyscale byte array of the frame being recognised. No `ImageCapture` use case is bound, so no JPEG is ever produced, and nothing is written under the cache or files directories.
- A picked photo is opened through `ContentResolver.openInputStream` for decoding and never copied.
- The `TessBaseAPI` instance is released when the scanning screen leaves the composition; the byte array is dropped with it.
- The recognised text stays in the view model only as the interpretation's `rawText` for the banner's sheet and is dropped with the draft when the flow closes.
- The tessdata copy under `filesDir/ocr/` contains no user data.
- The app lock treats the scanning screen like any other screen: leaving the app locks it, and the camera is unbound with the lifecycle.

### D7: The complete permission and manifest accounting

This is the whole list. Anything not on it is a defect, and D8 is the check.

**Correction made at apply time (task 1.1).** Writing the allow-list against the real merged manifest showed that the app has never shipped with only the seven permissions its own manifest declares. Five more arrive through manifest merging and have been in every release so far: `USE_BIOMETRIC` and `USE_FINGERPRINT` from `androidx.biometric` (the app lock), `WAKE_LOCK` and `ACCESS_NETWORK_STATE` from `androidx.work` (the alarm watchdog), and `${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, a signature-level permission `androidx.core` defines for the app itself. None of them is `INTERNET`, and Pillsner still opens no connection; `ACCESS_NETWORK_STATE` is WorkManager reading whether the network is up for constraints Pillsner never sets. Stripping them with `tools:node="remove"` is exactly the patching PR #46 showed to be the wrong instinct, so the allow-list discloses all twelve with their contributors, the README explains them, and the table below is corrected. The lesson of D8 stands: nobody had read the merged manifest until a build task did.

| Item | Before | After |
| --- | --- | --- |
| Declared by Pillsner's manifest | `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` (≤32), `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SHORT_SERVICE`, `USE_FULL_SCREEN_INTENT` | the same seven **plus `CAMERA`** |
| Contributed by libraries (see the correction above) | `USE_BIOMETRIC`, `USE_FINGERPRINT` (biometric), `WAKE_LOCK`, `ACCESS_NETWORK_STATE` (work), `nl.hexmaster.pillsner.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (core) | unchanged; CameraX and Tesseract4Android contribute none (task 2.4) |
| `CAMERA` | not declared | declared; runtime permission requested only when the user chooses "Scan with camera", after an in-app rationale |
| `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` | not declared | not declared (system photo picker) |
| `INTERNET` | not declared | not declared, and the guard fails the build if a dependency reintroduces it (`ACCESS_NETWORK_STATE` is WorkManager's, in the library row below) |
| `<uses-feature>` | none for camera | `android.hardware.camera.any` with `required="false"` (install on camera-less devices unaffected) |
| `<queries>` | vendor power-manager packages | unchanged |
| `<provider>`, `<service>`, `<receiver>`, `<activity>` | Pillsner's own, plus what WorkManager, Play services and androidx.startup already merged in | plus one disabled, unexported `<service>` from `camera-core` (`androidx.camera.core.impl.MetadataHolderService`, carrying the Camera2 default-config meta-data). Task 2.4 read the merged debug and release manifests: that service is CameraX's only contribution, and the Tesseract4Android AAR manifest holds nothing but `uses-sdk` |
| Runtime prompts by Pillsner | notification permission | plus the camera permission, in context only |
| New AndroidX libraries | none | `androidx.camera` core, camera2, lifecycle, compose, and transitively `camera-camera2-pipe`, `featurecombinationquery`, `androidx.camera.viewfinder` compose and core (all 1.6.2) |
| Native libraries | none of Pillsner's own | Tesseract, Leptonica, libjpeg, libpng for arm64-v8a, armeabi-v7a, x86, x86_64 (bundle splits per ABI) |
| Assets | fonts, legal documents | plus `tessdata/eng.traineddata` |
| Play Data safety | no data collected or shared | unchanged: camera frames are processed in memory and neither stored nor shared |
| Play listing | no camera permission | shows the camera permission; the README row explains it |
| Backup rules | app-lock DataStore excluded | plus `files/ocr/` excluded |

The README permission table gets a `CAMERA` row: "Only to read a medicine label you hold in front of it, and only after you choose to scan one. Frames are read in the app and thrown away; no photo is saved and nothing is sent anywhere. Decline it and you can still pick an existing photo or type everything by hand."

The public privacy statement (`PRIVACY.md`) changes in the same release, as its own section 13 promises: version 2 drops the "no camera access" claim, explains the camera in its own bullet, names CameraX and Tesseract4Android among the libraries, lists every permission the installed app carries (the same twelve plus `CAMERA` as the allow-list), and says the recognised text is never logged. Its effective date is the day of the release that carries this change (added at apply time, review on #86).

### D8: The build guard makes D7 enforceable

A Gradle task in `app/build.gradle.kts`, `verifyManifestGuard<Variant>`, runs after `process<Variant>Manifest` for every variant and is wired into `check`, `assemble<Variant>` and `bundle<Variant>`, so both CI (`assembleDebug`) and the release workflow run it. It:

1. Parses the merged manifest and asserts the set of `uses-permission` names equals the disclosed list held in `app/manifest-allowlist.txt` (one permission per line, comments allowed; this file is the human-readable contract and is referenced from the README). After this change the list holds thirteen permissions: the seven Pillsner declares itself, the five its libraries merge in (see the correction under D7), and `CAMERA`.
2. Walks the variant's runtime classpath and fails on any artifact whose group is `com.google.firebase`, `com.google.mlkit` or `com.google.android.datatransport`, on `androidx.camera:camera-mlkit-vision` by name, and on `com.google.android.gms` outside an explicit allow-list of the wearable artifacts the phone app already uses and their existing transitive base.
3. Computes the SHA-256 of the resolved Tesseract4Android AAR and compares it with the value pinned on an `artifact` line in `app/manifest-allowlist.txt`.

The failure message names the offending permission, artifact or checksum. The task has no network access and adds well under a second to the build. It is deliberately simple XML and classpath inspection, not a plugin.

**Alternative considered: Gradle dependency verification metadata.** It is all-or-nothing for every artifact in the build and would need checksums for hundreds of AndroidX artifacts, with churn on every bump. The single pinned checksum for the one artifact that comes from an unusual source is the proportionate version.

## Risks / Trade-offs

- [Risk] Tesseract reads camera frames worse than ML Kit did, especially on curved bottles or poor light → Mitigation: continuous frames give many chances rather than one, the two-frame agreement rule filters single misreads, the design never trusts the read (D5 banner, ordinary validation), task 8.4 measures accuracy on real labels in six languages before the pull request opens, `PSM_SPARSE_TEXT` and per-language models are the two documented knobs, and the ONNX pipeline in D1 is the recorded fallback if the measurement is unacceptable.
- [Risk] Per-frame recognition is too slow on low-end phones, so the scan feels stuck → Mitigation: analysis runs at 1280 × 960 cropped to the guide, on its own thread, dropping frames while busy; the preview never stalls; the hints at 8 and 20 seconds and the shutter give the user a way out; task 8.5 measures per-frame time on the slowest supported test device.
- [Risk] A CameraX artifact contributes manifest entries beyond `CAMERA` and the camera feature → Mitigation: the guard fails the build on any permission not on the allow-list; task 2.4 reads the merged manifest and records what the libraries contribute.
- [Risk] Users decline the camera permission and assume the feature is gone → Mitigation: the snackbar names "Choose a photo" as the alternative, and the permanently-denied path offers the settings page.
- [Risk] JitPack is unavailable when a release builds → Mitigation: Gradle's dependency cache holds the artifact once resolved; the guard's pinned checksum ensures a re-resolved artifact is byte-identical; the repository filter means no other artifact can ever depend on JitPack.
- [Risk] The interpretation guesses a schedule wrongly and the user saves it, so reminders fire at the wrong times → Mitigation: the form lists schedules with their descriptions, Save requires reviewing the form, the default times are the same visible 08:00-based defaults the editor uses, and unusual frequencies produce no schedule rather than a guess.
- [Risk] Package size grows by CameraX, the native libraries and the model → Mitigation: measured and recorded in task 8.6; the model is one 4 MB file; ABI splits deliver one set of native libraries per device; called out in the CHANGELOG.
- [Risk] A label in Dutch, Spanish or Portuguese reads badly with the English model → Mitigation: measured per language in task 8.4; adding a model is a data-only change; vocabulary matching is accent-insensitive so a wrongly read accent does not break a unit or frequency match.
- [Risk] The guard's permission allow-list becomes a thing people edit to make the build pass → Mitigation: the file is small, is referenced from the README's permission table as the source of truth, and any edit to it is visible in review; the Copilot review instructions are told to flag changes to it.
- [Risk] A viewfinder is a purely visual interaction → Mitigation: the shutter, the spoken hints, the "Label read" announcement and the picked-photo path keep the feature usable with TalkBack; the manual form is always the baseline.

## Migration Plan

No schema change and no data migration. The change ships as one feature branch. Rollback is removing the "Scan a label" button, the scanning screen, the recogniser, the CameraX and Tesseract dependencies, the repository entry, and the `CAMERA` line from the manifest and the allow-list together; the guard task and the README structure can stay, since they describe the app as it is with or without the feature. The removed `medicine-label-ocr-scan` change folder is not restored on rollback; it re-proposed the approach this design rejects.

## Open Questions

- Whether recognition quality on real labels in each of the six languages is acceptable with the English model alone. Task 9.4 answers this with a measurement on a physical device before the pull request opens and records the outcome here. **Not yet measured at apply time**: the emulator has no real label to hold up. What is known: on the x86_64 emulator the real engine reads a drawn label (`LabelTextRecogniserTest`) and the interpretation returns exactly the expected fields.
- Per-frame recognition time on the slowest supported test device, and whether 1280 × 960 is the right analysis resolution. Task 9.5 records it here. **First data point (29 September 2026, x86_64 emulator, not a slow device)**: engine open 145 to 242 ms, one 1000 × 390 greyscale frame recognised in 80 ms.
- The per-device package size increase. **Measured (29 September 2026, task 9.7)** with bundletool 1.18.3 `get-size total --dimensions=ABI` on the release bundles of `development` (bd75ecb) and this branch: download size per device rose from about 3.10 MB to 9.00 MB on arm64-v8a (+5.9 MB), 8.68 MB on armeabi-v7a (+5.6 MB), 9.28 MB on x86_64 (+6.2 MB) and 9.26 MB on x86. The English model is 4.1 MB of that; the rest is the native Tesseract, Leptonica, libjpeg and libpng libraries for one ABI and the CameraX classes.
