## Context

The Usage history screen (`medicine-usage-history`) already loads, for one medicine and a selected period, everything this export needs: the scheduled/taken/skipped/missed/unanswered counts, the adherence percentage, the medicine's name and schedule description, and the per-dose records the charts are built from. That view model is the single source of truth this design reuses rather than re-querying.

Pillsner has no PDF or file-sharing code today. `android.graphics.pdf.PdfDocument` is part of the Android SDK (`android.graphics.pdf`, API 19+) and draws a page as an ordinary `Canvas`, which is enough for a one-to-two-page report of headings, key-value figures and a table — no layout engine, no HTML-to-PDF, no third-party library needed. Sharing a file that lives in the app's own storage with another app requires a `FileProvider` (`androidx.core.content.FileProvider`, already transitively available via AndroidX core) rather than a raw `file://` URI, which `FileUriExposedException` forbids from API 24 onward.

## Goals / Non-Goals

**Goals:**

- Turn the figures already on the Usage history screen into a document the user can hand to another person, without changing what those figures mean or how they're computed.
- Keep the export entirely on-device: generate a file, hand it to the OS share sheet, and let the user decide where it goes.
- Default to the least sensitive shape of the report (the summary, no dose-level detail) and require an explicit opt-in for more.
- Reuse the existing period selection and view-model data; no new repository queries beyond what the screen already loads.

**Non-Goals:**

- No new adherence calculation. If the summary screen and the exported report ever disagree, that is a bug in the export, not a second source of truth.
- No automatic, scheduled, or background sending of the report to anyone.
- No aggregate report across multiple medicines in one file — one medicine, one period, one document, matching the screen it's generated from.
- No persistent "export history" or list of past exports inside the app.

## Decisions

### D1. Report content comes from the existing `MedicineHistoryUiState`, not a new query

The export action reads the same `MedicineHistoryUiState` the Usage history screen is already displaying for its current medicine and period. Today that state (`MedicineHistoryUiState.kt`) holds only `medicineName`, `history` and `timeDeviation`; the per-dose list is read inside `MedicineHistoryViewModel`'s `combine` block but discarded rather than retained, and there is no schedule text at all. This change extends `MedicineHistoryUiState` with two fields the `combine` block already has the inputs for: `doses: List<Dose>` (the same list the `combine` already receives from `doseRepository.observeHistoryFor(...)`, simply kept instead of dropped) and `scheduleDescription: String` (the medicine's schedule, formatted with the existing `ScheduleDescriptionFormatter` from the `Medication` already read in `init`). Neither addition triggers a new repository query — both values are already being fetched for the screen's own use; only the retention changes.

A dedicated `AdherenceReportBuilder` (domain layer, no Android dependency) then maps that extended state into a small `AdherenceReport` data class: medicine name, schedule text, period label, scheduled/taken/skipped/missed/unanswered counts, adherence percentage, and — only when the caller asks for it — the ordered list of per-dose rows (due time, answered time, outcome). This keeps "what the report says" mechanically tied to "what the screen already says," and keeps the mapping unit-testable without Android.

### D2. PDF via `PdfDocument`, not a third-party library

A `PdfExportRenderer` (data/UI-adjacent layer, since it touches `android.graphics`) takes an `AdherenceReport` and draws it onto one or more `PdfDocument.Page` canvases: a header (medicine, schedule, period, generated-on date), the summary figures, the breakdown, and — if included — a simple table of dose rows, paginating to a new page when the table overflows. This is deliberately plain, print-oriented layout (no charts reproduced from Compose — the bar charts are a screen affordance, not something worth the complexity of rasterizing into the PDF). Considered and rejected: rendering the existing Compose screen to a bitmap and embedding that — it would fight `PdfDocument`'s vector text output, produce a report that doesn't paginate, and couple report layout to screen layout.

### D3. CSV as a second, independent writer over the same `AdherenceReport`

A small `CsvExportWriter` takes the same `AdherenceReport` and writes the per-dose rows (or, if none were included, just the summary figures) as comma-separated values with a header row. CSV always includes whatever level of detail the user opted into for the PDF — there's one "include individual doses" toggle for both formats, not two.

### D4. Files live in a dedicated cache subdirectory exposed by one `FileProvider`

Both writers save to `context.cacheDir/exports/`, named `<medicine>-<period>-<timestamp>.pdf` / `.csv` using a sanitised, non-identifying token rather than the raw medicine name in the filename (the file's *content* names the medicine; the *filename* travels more casually — e.g. visible in a share-target app's recents — so it stays generic, e.g. `pillsner-report-<timestamp>.pdf`). A single `FileProvider` entry in the manifest, scoped via `file_paths.xml` to that `exports/` cache subdirectory only, grants the receiving app temporary read access to the shared file via `Intent.FLAG_GRANT_READ_URI_PERMISSION` on the `ACTION_SEND` intent — nothing else under the app's storage is reachable through it.

Exported files are cache, not user data: nothing prunes them proactively during this change, but they live under `cacheDir` so the OS may reclaim the space under storage pressure, and they are never surfaced inside the app as something to browse, rename or delete — the only way to get one is to generate it fresh from "Share with your doctor."

### D5. Dose detail is opt-in, chosen once per export

The share action opens a small confirmation dialog: period is already fixed (whatever the screen has selected), a "Include individual doses" switch defaults off, and a choice of PDF or CSV (PDF preselected, since it's the one meant to be handed over or printed; CSV is the explicit secondary option named in the proposal). Confirming builds the report, writes the chosen file, and launches `ACTION_SEND` with a chooser title from a string resource. There is no separate "preview" step — the generated PDF, once shared, opens directly in whatever viewer the user picks, which is preview enough without Pillsner building its own.

## Risks / Trade-offs

- **[Risk]** A PDF built by hand-drawing on a `Canvas` is more code than a layout-driven approach → **Mitigation**: the report is intentionally simple (headings, key-value pairs, one table), well within what direct `Canvas` drawing handles cleanly; complexity is capped by keeping charts out of the PDF entirely (see D2).
- **[Risk]** A stale cached export could be re-shared by mistake if a filename were reused → **Mitigation**: every export writes a fresh, timestamped file; nothing is overwritten or reused across exports.
- **[Risk]** Granting URI read access, even scoped and temporary, is the app's first time exposing a file to another process → **Mitigation**: `FileProvider` plus `file_paths.xml` scoped to exactly the `exports/` cache subdirectory, `FLAG_GRANT_READ_URI_PERMISSION` only, no `FLAG_GRANT_WRITE_URI_PERMISSION`, and no broader `<cache-path>` declaration than that one subdirectory.
- **[Risk]** Large per-dose appendices (three months, multiple doses a day) could produce an unwieldy PDF → **Mitigation**: `PdfExportRenderer` paginates the dose table across pages rather than shrinking it illegibly; this is a report meant to be printed and read, not a full database dump.

## Migration Plan

No schema or data migration: this change adds a rendering/export path over data that already exists. Manifest gains one `<provider>` entry (`FileProvider`) and one new `res/xml/file_paths.xml`. No existing behaviour changes; the feature is additive and reachable only through the new "Share with your doctor" action.
