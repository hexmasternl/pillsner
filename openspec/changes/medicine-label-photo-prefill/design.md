## Context

The Add medicine form (`ui/medicines/form/`) is a flat draft (`MedicationFormDraft`: name, dose text and unit, used since, use until, prescriber, schedules) owned by `MedicationFormViewModel`, shared between the form and the schedule editor through the flow's navigation graph entry, and persisted through `SavedStateHandle` so it survives rotation and process death. Schedules are the three domain shapes in `Schedule` (every N days at clock times, on weekdays at clock times, every N hours from a first dose). Dependency injection is the hand-written `AppContainer`. The manifest deliberately declares no `INTERNET` permission, and the README's permission table says so as a headline.

### What happened the first time

The archived change `medicine-add-label-scan` (issue #33, PR #38, shipped as 1.1.0) used `com.google.mlkit:text-recognition` with its bundled Latin model. That artifact transitively pulled in Firebase installations and Google data-transport components. Their own manifests declared `INTERNET` and `ACCESS_NETWORK_STATE` and registered content providers, and manifest merging added all of it to the shipped app without anyone changing Pillsner's own manifest. The result was a package that was not accepted as safe to install, and that on devices where it did install crashed at launch inside Google Play's integrity wrapper (`com.pairip.application.Application`, a `NullPointerException`). PR #46 stripped the two permissions with `tools:node="remove"`; the crash remained. PR #47 reverted `main` to the 1.0.2 tree. The exact mechanism of the crash was never isolated, which is precisely why this design does not try to patch around it: it removes the entire class of cause.

Two lessons shape everything below:

1. A dependency is not "offline" or "safe" because its documentation says so. What matters is what it contributes to the merged manifest and the dependency graph, and that has to be checked by the build, not by reading.
2. A permission that the app does not declare cannot leak. Every capability here is designed to need none.

## Goals / Non-Goals

**Goals:**

- Read a medicine label photo entirely on the device, with no model download, no network permission and no Google Play services, Firebase or ML Kit surface in the app.
- Pre-fill name, default dose, one or more schedules, used since and use until from the recognised text, then hand the user the ordinary form to review and correct.
- Add zero permissions. Make the complete list of manifest and dependency changes explicit in this document, in the README, and in a build check that fails when reality drifts from it.
- Never retain the photo, and never log the recognised text.
- Keep the interpretation logic pure Kotlin, unit-tested against labels in all six app languages.
- Degrade to today's manual form on a device without a camera app, when the user cancels, or when nothing usable is recognised.

**Non-Goals:**

- No drug database, no interaction or dosage checking, no medical interpretation beyond reading what is printed.
- No live viewfinder or in-app camera preview. The phone's camera app is used on purpose (see D2).
- No scan in edit mode; an existing medicine with recorded doses is not overwritten from a photo.
- No stock quantity, expiry date or prescriber pre-fill. The stock feature has its own form, and prescriber is a five-value choice the user picks in one tap.
- No non-Latin scripts. All six app languages are Latin-script.
- No dynamic delivery of models or native libraries. Everything ships in the base install so the feature works offline from the first launch.

## Decisions

### D1: Tesseract 5 through Tesseract4Android, not ML Kit, not a cloud API, not a hand-rolled model pipeline

The recogniser is Tesseract OCR 5 (LSTM engine) with Leptonica, packaged for Android by [Tesseract4Android](https://github.com/adaptech-cz/Tesseract4Android) (`cz.adaptech.tesseract4android:tesseract4android-openmp`, Apache 2.0). It is a single AAR of native code plus a thin Java API. Its only declared Java dependency is `androidx.annotation`, it registers no components in its manifest, and it has no relationship with Google Play services, Firebase or ML Kit. The OpenMP flavour is chosen because Pillsner runs exactly one recogniser instance at a time and wants it to use every core.

Why not the alternatives:

- **ML Kit (bundled or unbundled)**: this is the library that caused the 1.1.0 failure. Both flavours bring Firebase and data-transport transitively. Rejected outright.
- **A TensorFlow Lite / LiteRT or ONNX Runtime pipeline with an open OCR model (for example PaddleOCR)**: better accuracy on scene text and available from Maven Central, but it needs a detection model, a recognition model, custom post-processing of the detector's probability map (connected components, box merging, cropping, CTC decoding) and a dictionary file, all written and maintained in this repository. That is a lot of clever code in an app whose rule is "someone reading a bug at 2 a.m. should be able to follow it". Kept as the documented fallback if Tesseract's accuracy on real labels proves unacceptable (task 8.4 measures it).
- **Cloud OCR**: needs `INTERNET`. Rejected outright.
- **`tess-two`**: unmaintained since 2019, Tesseract 3. Rejected.

**Source repository.** Tesseract4Android is published through JitPack, not Maven Central. Pillsner's `settings.gradle.kts` uses `FAIL_ON_PROJECT_REPOS`, so JitPack is added there, with a content filter that admits only the group `cz.adaptech.tesseract4android`, so nothing else can ever resolve from it. Because JitPack builds from a tagged GitHub source, the SHA-256 of the resolved AAR is pinned in the guard task (D8) and the build fails if it changes. The version is pinned in the version catalog like every other dependency.

**Trained data.** Tesseract reads `.traineddata` files from a directory on disk. Pillsner bundles `eng.traineddata` from the `tessdata_fast` set (about 4.1 MB) under `app/src/main/assets/tessdata/` and copies it to `filesDir/ocr/tessdata/` before the first scan (a version marker file next to it makes an app update replace it). That directory is excluded from cloud backup and device transfer in both backup rule files; it is derived data that any device can recreate from the app's own assets.

Why English only, at first: the parts of a label that matter (medicine name, numbers, units, dates) are language-neutral, and the vocabulary in D4 matches unit and frequency words accent- and case-insensitively, so "comprimé" read as "comprime" still matches. The per-language `tessdata_fast` files for the other five app languages together add about 13 MB (nld 6.0, spa 2.3, por 2.0, deu 1.5, fra 1.1). Task 8.4 measures recognition on real labels in each language; if a language performs badly, adding its file is a data change and a one-line list in the recogniser, not a design change. The `Latin` script model (89 MB) is out of the question.

### D2: The phone's own camera app takes the photo, so Pillsner declares no camera permission

The "Scan a label" action offers two paths:

- **Take a photo** launches `ActivityResultContracts.TakePicture()` with a content URI from Pillsner's own `FileProvider`, pointing at a fresh file under `cacheDir/label-scan/`. The system camera app takes the picture under its own permission and writes it to that URI. Android only requires the `CAMERA` permission for this intent when the calling app declares it, so Pillsner does not declare it, and no runtime prompt is ever shown by Pillsner.
- **Choose a photo** launches `ActivityResultContracts.PickVisualMedia()`, the system photo picker, which needs no storage or media permission on any supported Android version.

Whether a camera app exists is checked with `PackageManager.resolveActivity` for `MediaStore.ACTION_IMAGE_CAPTURE`. From Android 11 that needs a `<queries>` declaration for the intent, which is the one manifest addition beyond the provider. When no camera app resolves, only "Choose a photo" is offered.

The `FileProvider` is `androidx.core.content.FileProvider` (already a dependency through `androidx.core`), authority `${applicationId}.labelscan`, `exported="false"`, `grantUriPermissions="true"`, with a `paths` resource that exposes only `cache-path name="label-scan" path="label-scan/"`. Nothing else in the app's storage is reachable through it.

**Alternative considered: CameraX with an in-app viewfinder and a framing guide.** Better guidance for the user, but it requires the `CAMERA` permission, a new runtime prompt, a new AndroidX library family and a preview screen. The permission is exactly the kind of surface this change exists to avoid, and the system camera app is already accessible with TalkBack and large fonts. If real-world use shows that people cannot get a usable photo without framing help, that is a follow-up change with its own permission discussion.

### D3: One image, one recogniser, off the main thread

`LabelTextRecogniser` (data layer, Android-dependent) does, for one scan:

1. Read the image's EXIF orientation with the framework's `android.media.ExifInterface` (no new dependency) and decode it with `BitmapFactory` using `inSampleSize` so the longer side is at most 2,000 pixels. Bounding the size bounds memory and recognition time.
2. Rotate to upright, convert to greyscale. Leptonica inside Tesseract does its own binarisation.
3. Create a `TessBaseAPI`, initialise it with the `filesDir/ocr/tessdata/` path and language `eng`, page segmentation mode `PSM_AUTO`, hand it the bitmap and read the result through the result iterator at text-line level, producing a list of `RecognisedLine(text, confidence)` in reading order. `PSM_SPARSE_TEXT` is the documented switch to try in task 8.4 if labels with scattered text read badly.
4. Recycle the bitmap and the API in a `finally`.

The whole call runs on `Dispatchers.Default` inside `viewModelScope` and is cancellable; a cancellation stops the Tesseract call through its `stop()` hook. One `TessBaseAPI` per scan, initialised each time: initialisation costs a fraction of a second and avoids holding native memory for the life of the process.

Nothing here logs the bitmap, the text or the confidence. The only log lines are debug-level "scan started", "scan finished in N ms" and failure classes without payload.

### D4: Interpretation is a pure domain function with a multilingual vocabulary

`InterpretLabelText(lines: List<RecognisedLine>, today: LocalDate): LabelInterpretation` lives in `domain/labelscan/`, imports nothing from Android or Tesseract, and is where all the rules live. It is deterministic and unit-tested per rule and per language.

`LabelInterpretation` holds: `name: String?`, `defaultDose: Quantity?`, `schedules: List<Schedule>`, `usedSince: LocalDate`, `useUntil: LocalDate?`, `rawText: String`. Absent means "leave the field as it is".

**Vocabulary.** One table, not keyed by the app language, because the label was printed by the pharmacy in its language, not the phone's. Matching is case-insensitive and accent-insensitive (Unicode NFD, marks stripped). The table maps words in English, Dutch, German, French, Spanish and Portuguese to:

- **Units** (`DoseUnit`): mg/milligram; g/gram; mcg/µg/ug/microgram; ml/mL/millilitre; tablet, tabletten, Tablette(n), comprimé(s), comprimido(s), tab(s); capsule(s), kapsel(n), gélule(s), cápsula(s); drop(s), druppel(s), Tropfen, goutte(s), gota(s); puff(s), pufje(s), inhalatie(s), Hub, Sprühstoß, bouffée(s), inhalación, inalação; unit(s), eenheid/eenheden, Einheit(en), unité(s), unidad(es), IE, IU, UI.
- **Frequency words**: "per day" forms (daags, per dag, maal daags, times a day, daily, täglich, mal täglich, fois par jour, veces al día, vezes ao dia), "every N hours" forms (om de N uur, every N hours, alle N Stunden, toutes les N heures, cada N horas, a cada N horas), "every other day" forms (om de dag, every other day, jeden zweiten Tag, tous les deux jours, cada dos días, dia sim dia não), day-part words (morning/noon/evening/night in all six languages), and the box notation `1-0-1`, `1-1-1`, `1-0-0-1` (morning, noon, evening, optional night).
- **Duration words**: day, week (and plurals) in all six languages, with "for/during" forms (gedurende, for, lang, pendant, durante), and "until" forms (tot, t/m, until, bis, jusqu'au, hasta, até).
- **Noise lines** to skip when choosing the name: pharmacy words (apotheek, pharmacy, Apotheke, pharmacie, farmacia, farmácia), salutations (dhr, mevr, Mr, Mrs, Ms, Herr, Frau, M., Mme, Sr., Sra.), phone-number and postcode shapes, lines that are only a date, and lines that match a frequency pattern.

**Rules, in order:**

1. **Dose tokens.** Every `<number><unit>` match, where the number is an integer or a decimal with `.` or `,` and the unit is a vocabulary word, becomes a `Quantity`. Strength tokens are those in a mass or volume unit (mg, g, mcg, ml). Count tokens are those in a form unit (tablet, capsule, drop, puff, unit).
2. **Default dose** is the first strength token on the label. When there is none, it is the first count token that sits in an instruction line (a line that matches a frequency pattern), such as "1 tablet" in "take 1 tablet twice a day". When there is neither, the default dose is absent.
3. **Schedule amount** is the count token in the instruction line when there is one; otherwise the default dose. A schedule is produced only when both an amount and a frequency were found.
4. **Frequency to schedule**, using the app's existing shapes:
   - N times a day, for N from 1 to 4, becomes `EveryNDays(interval 1)` with fixed default times: 1 → 08:00; 2 → 08:00, 20:00; 3 → 08:00, 14:00, 20:00; 4 → 08:00, 12:00, 16:00, 20:00. "once" and "twice" words count as 1 and 2.
   - N times a day for N above 4 becomes `EveryNHours(24 / N, from 08:00)` when 24 / N is one of the intervals the schedule editor offers (1, 2, 3, 4, 6, 8, 12, 24); otherwise no schedule.
   - Every N hours becomes `EveryNHours(N, from 08:00)` when N is one of those intervals; otherwise no schedule.
   - Every other day becomes `EveryNDays(interval 2, 08:00)`.
   - Day-part words become `EveryNDays(interval 1)` with the matching slots: morning 08:00, noon 13:00, evening 18:00, night 22:00.
   - Box notation `a-b-c[-d]` maps the non-zero slots to the same four slot times. When every non-zero digit is the same, that is one schedule with that count as the amount (in the count unit, or the default dose's unit when there is no count unit). When the digits differ, one schedule is produced per distinct non-zero digit, each with its own times.
   - The weekdays shape is never produced; labels do not carry it in a form worth guessing.
   All produced schedules are validated by the same domain constructors the editor uses; anything they reject is dropped rather than patched.
5. **Name.** Candidate lines are all lines that are not noise lines, with every dose token, pack-size token ("30 tabletten", "N st", "N pcs") and trailing punctuation removed, whitespace collapsed, and at least three letters left. The name is the first candidate that contains a strength token in the original line (name and strength share a line on most labels), otherwise the first candidate. The result is truncated to 60 characters, the form's name field length. Case is kept as recognised.
6. **Dates.** Recognised in the numeric forms `dd-mm-yyyy`, `dd/mm/yyyy`, `dd.mm.yyyy`, `yyyy-mm-dd` and the same with a two-digit year. A month-and-year only form (`03/2028`) is never a date candidate: that is how expiry dates are printed. Used since is the most recent candidate that is on or before today and not more than 365 days before it; when there is none, it is today. Any candidate after today is treated as an expiry or "until" date, never as a start.
7. **Use until.** In priority order: a date candidate that follows an "until" word and is after used since; otherwise a duration "N days" or "N weeks" after a "for/during" word or standing alone in an instruction line, giving used since plus N days minus one (a 7-day course that starts today ends on the seventh day, inclusive) or plus 7·N days minus one for weeks; otherwise absent. A result before used since is discarded.
8. **Raw text** is the recognised lines joined with newlines, for the "Show recognised text" sheet only.

Every rule is small and every rule has its own test, including fixtures for a Dutch pharmacy label, a German box with `1-0-1`, a French "1 comprimé matin et soir pendant 7 jours", a Spanish "cada 8 horas", a Portuguese "2 vezes ao dia durante 2 semanas" and an English "take one tablet twice daily for 10 days". Fixtures use invented medicine names and are not real people's labels.

### D5: The scan starts on the form and lands in the shared draft

"Scan a label" sits at the top of the Add medicine form, above the name field, and only in `MedicationFormMode.Add`. It is an outlined button with a camera icon (design system 8.4 secondary button). Tapping it opens a bottom sheet (8.12) with "Take a photo" and "Choose a photo", the first hidden when no camera app resolves.

The form's `MedicationFormNavigation` owns the two activity-result launchers (the same `rememberLauncherForActivityResult` pattern as `ui/home/NotificationPermissionRequest.kt`). The capture URI is created by the view model, kept in `SavedStateHandle` under its own key, and handed to the launcher, so a process death while the camera app is open still delivers the result to a view model that knows which file to read. On a result, the view model runs `LabelTextRecogniser` and then `InterpretLabelText` in `viewModelScope`, publishing `isScanning = true` while it works. The screen shows a modal progress state, "Reading the label…", with a Cancel action that cancels the job.

Applying the result:

- When the draft is untouched (equal to its initial state), the interpretation is applied at once.
- When the draft already has edits, the form asks "Replace what you have entered with what was read from the photo?" with Keep and Replace. Replace applies the interpretation; Keep discards it.
- Applying sets exactly the fields the interpretation carries: name; dose text (formatted with the form's `AmountParser`) and unit; schedules (replacing the list); used since; use until. Absent fields are left as they are. Prescriber and active flag are never touched. The draft is persisted through `DraftSaver` as after any edit, so the pre-fill survives rotation.
- The form then shows an attention banner (8.9) at the top: "Filled in from your label photo. Check every field before saving." with two actions, "Show text" and dismiss. "Show text" opens a sheet with the raw recognised text in body type, selectable, nothing else. The banner stays until dismissed or until the form closes; it is not shown again on rotation once dismissed (a flag in the saved state).
- When the interpretation carries nothing at all (no name, no dose, no schedule, and dates defaulted), the form is not changed and a snackbar says nothing readable was found. A decoding or recogniser failure gives a different snackbar ("The photo could not be read"). Neither names the medicine or quotes text.

Pre-filled values go through the ordinary validation on Save; there is no separate path, so a bad read surfaces as the existing field error.

**Alternative considered: a camera button on the Medicines screen that opens the form already filled (the entry point of the first attempt).** It saves one tap but needs route arguments for every field including a list of schedules, cannot use the shared draft, and gives the user no way to scan again from the form. Starting on the form keeps one code path and the legal-acceptance gating the form already has.

### D6: The photo lives only in `cacheDir/label-scan/` and only for the length of the scan

- Capture files are created under `cacheDir/label-scan/` with a random name and are the only files the `FileProvider` can serve. They are never registered with `MediaStore` and never written anywhere else.
- A picked photo is opened through `ContentResolver.openInputStream` for decoding and never copied.
- The capture file is deleted in a `finally` when the scan ends, whatever the outcome, and also when the user cancels the camera app.
- Because a `finally` does not run through a process kill, the directory is swept (every file deleted) in `PillsnerApplication.onCreate` and at the start of every scan. The window between a crash and the next start is the only time a file can exist, and during it the file is in the app's private cache, which no other app can read and which is excluded from backup by the platform.
- The decoded bitmap is recycled after recognition; the recognised text stays in the view model only as the interpretation's `rawText` for the banner's sheet and is dropped with the draft.
- The tessdata copy under `filesDir/ocr/` contains no user data.

### D7: The complete permission and manifest accounting

This is the whole list. Anything not on it is a defect, and D8 is the check.

| Item | Before | After |
| --- | --- | --- |
| Declared permissions | `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` (≤32), `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SHORT_SERVICE`, `USE_FULL_SCREEN_INTENT` | **Identical** |
| `CAMERA` | not declared | not declared (the camera app holds its own) |
| `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` | not declared | not declared (system photo picker) |
| `INTERNET`, `ACCESS_NETWORK_STATE` | not declared | not declared, and the guard fails the build if a dependency reintroduces them |
| `<uses-feature>` | none for camera | none (never declared, so install on camera-less devices is unaffected) |
| `<queries>` | vendor power-manager packages | plus one `<intent>` for `android.media.action.IMAGE_CAPTURE` |
| `<provider>` | none of Pillsner's own | one `androidx.core.content.FileProvider`, `exported="false"`, cache path `label-scan/` only |
| Runtime prompts by Pillsner | notification permission | unchanged |
| Native libraries | none of Pillsner's own | Tesseract, Leptonica, libjpeg, libpng for arm64-v8a, armeabi-v7a, x86, x86_64 (bundle splits per ABI) |
| Assets | fonts, legal documents | plus `tessdata/eng.traineddata` |
| Play Data safety | no data collected or shared | unchanged |
| Backup rules | app-lock DataStore excluded | plus `files/ocr/` excluded |

The README permission section gets one sentence under the table: scanning a label uses the phone's own camera app or photo picker, so Pillsner needs no camera, photo or storage permission, and the recognition runs inside the app without any network access.

### D8: The build guard makes D7 enforceable

A Gradle task in `app/build.gradle.kts`, `verifyManifestGuard<Variant>`, runs after `process<Variant>Manifest` for every variant and is wired into `check`, `assemble<Variant>` and `bundle<Variant>`, so both CI (`assembleDebug`) and the release workflow run it. It:

1. Parses the merged manifest and asserts the set of `uses-permission` names equals the disclosed list held in `app/manifest-allowlist.txt` (one permission per line, comments allowed; this file is the human-readable contract and is referenced from the README).
2. Walks the variant's runtime classpath and fails on any artifact whose group is `com.google.firebase`, `com.google.mlkit`, `com.google.android.datatransport`, or `com.google.android.gms` outside an explicit allow-list of the wearable artifacts the phone app already uses and their existing transitive base.
3. Computes the SHA-256 of the resolved Tesseract4Android AAR and compares it with the value pinned next to the version in the catalog (a `# sha256:` comment is not machine-checkable, so the value lives in `app/manifest-allowlist.txt` under an `artifact` line).

The failure message names the offending permission, artifact or checksum. The task has no network access and adds well under a second to the build. It is deliberately simple XML and classpath inspection, not a plugin.

**Alternative considered: Gradle dependency verification metadata.** It is all-or-nothing for every artifact in the build and would need checksums for hundreds of AndroidX artifacts, with churn on every bump. The single pinned checksum for the one artifact that comes from an unusual source is the proportionate version.

## Risks / Trade-offs

- [Risk] Tesseract reads phone photos worse than ML Kit did, especially on curved bottles or poor light → Mitigation: the design never trusts the read (D5 banner, ordinary validation), task 8.4 measures accuracy on real labels in six languages before the pull request opens, `PSM_SPARSE_TEXT` and per-language models are the two documented knobs, and the ONNX pipeline in D1 is the recorded fallback if the measurement is unacceptable.
- [Risk] JitPack is unavailable when a release builds → Mitigation: Gradle's dependency cache holds the artifact once resolved; the guard's pinned checksum ensures a re-resolved artifact is byte-identical; the repository filter means no other artifact can ever depend on JitPack.
- [Risk] The interpretation guesses a schedule wrongly and the user saves it, so reminders fire at the wrong times → Mitigation: the banner names the schedule in words (the form already lists schedules with their descriptions), Save requires reviewing the form, the default times are the same visible 08:00-based defaults the editor uses, and unusual frequencies produce no schedule rather than a guess.
- [Risk] Package size grows by the native libraries and the model → Mitigation: measured and recorded in task 8.6; the model is one 4 MB file; ABI splits deliver one set of native libraries per device; called out in the CHANGELOG.
- [Risk] A label in Dutch, Spanish or Portuguese reads badly with the English model → Mitigation: measured per language in task 8.4; adding a model is a data-only change; vocabulary matching is accent-insensitive so a wrongly read accent does not break a unit or frequency match.
- [Risk] The guard's permission allow-list becomes a thing people edit to make the build pass → Mitigation: the file is small, is referenced from the README's permission table as the source of truth, and any edit to it is visible in review; the Copilot review instructions are told to flag changes to it.
- [Risk] The camera app writes an unexpectedly large photo → Mitigation: `inSampleSize` decoding bounds the bitmap to 2,000 pixels on the long side regardless of the source size.
- [Trade-off] No in-app viewfinder means no framing guidance → Accepted for zero permissions; revisit only with evidence from use.

## Migration Plan

No schema change and no data migration. The change ships as one feature branch. Rollback is removing the "Scan a label" button, the recogniser, the dependency and the repository entry; the guard task, the allow-list and the README wording can stay, since they describe the app as it is with or without the feature. The removed `medicine-label-ocr-scan` change folder is not restored on rollback; it re-proposed the approach this design rejects.

## Open Questions

- Whether recognition quality on real labels in each of the six languages is acceptable with the English model alone. Task 8.4 answers this with a measurement before the pull request opens and records the outcome in this section.
- The per-device package size increase. Task 8.6 records it here.
