## Context

Medicines are stored in Room with name, default dose, used since/until, prescriber and their schedules (`medicine-add`, `medicine-details`). There is no stock/remaining-quantity concept anywhere in the current specs, so this design does not build on or reuse a refill-warning mechanism — none exists yet. It does establish a presentation pattern (a per-medicine heads-up state, shown on the tile and on the details screen) that a future refill feature can reuse, kept distinct by wording and icon so the two are never conflated.

## Goals / Non-Goals

**Goals:**
- Let the user record an optional expiry date per medicine.
- Classify a medicine's expiry state as none, approaching, or past, and surface that state on the Medicines screen and the details screen.
- Keep expiry fully inert with respect to reminders, dose generation and scheduling.

**Non-Goals:**
- No stock/remaining-quantity tracking or refill warning — out of scope, a separate future proposal.
- No barcode/label scanning to read the expiry date (tracked separately as issue #33's OCR proposal, which this pairs with naturally but does not depend on).
- No medical judgement about whether an expired medicine is still safe to take.
- No per-batch or per-dose expiry — one expiry date per medicine, same granularity as every other medicine-level field.

## Decisions

- **Data model**: add `expiryDate: LocalDate?` to the `Medication` and `NewMedication` domain models and to the Room entity (nullable, same representation as `useUntil`), so the Add and details forms' save paths can carry it into the repository the same way every other field already does. Room migration adds the column with a `NULL` default; existing rows are unaffected. A migration test asserts the new column exists and existing rows survive the migration with `expiryDate = null`.
- **Expiry state classification**: a pure domain function `expiryState(expiryDate: LocalDate?, today: LocalDate, approachingWindow: Duration = 30.days): ExpiryState` returning `NONE`, `APPROACHING`, or `PAST`. Thirty days is chosen as a fixed, non-configurable lead time for this first version — simple, predictable, and avoids a new settings surface; a configurable window is left as a follow-up if requested. This mirrors the existing domain-layer pattern of pure, Android-free functions covering date-boundary edge cases (unit-tested around midnight, month and year boundaries, and leap days per CLAUDE.md's testing expectations).
- **Validation**: expiry date, when set, must not be before "used since" — mirrors the existing "use until must not be before used since" rule in `medicine-add`. No relationship is enforced against "use until": a medicine can be marked inactive (used until a date) independently of its pack's physical expiry.
- **Presentation**: `APPROACHING` and `PAST` render as a small badge/icon on the medicine tile (Medicines screen) and as inline text on the details screen, using distinct wording ("Expires soon" / "Expired") from any future refill wording, and never using colour alone to distinguish the two states from each other or from the default tile (per the existing accessibility rule that inactive tiles "MUST NOT rely on colour alone" — the same bar applies here). Exact tokens, icon and copy are decided by the `pillsner-designer` agent against `docs/design-system.md` during implementation, not fixed in this design.
- **No scheduling interaction**: the dose-generation and reminder pipelines (`reminder-scheduling`, `medicine-reminders`) are untouched; expiry state is read only by the UI layer at render time, never consulted when generating or delivering a dose.

## Risks / Trade-offs

- [A fixed 30-day window may not suit every medicine (e.g. a short-course antibiotic vs. a multi-month supply)] → Acceptable for v1 since it mirrors how CLAUDE.md already treats this kind of lead time as a detail to settle in design rather than a blocking requirement; revisit if user feedback asks for it.
- [Adding a badge to an already-dense tile risks visual clutter] → Only one expiry badge state is ever shown per tile (approaching or past, never both), and it is omitted entirely when there is no expiry date, so unaffected medicines are unchanged.
- [Users may confuse "use until" (when a medicine is deactivated) with "expiry date" (when the physical pack goes bad)] → Distinct field labels and inline help text on the form; details screen shows both when present so the distinction is visible, not implicit.

## Migration Plan

1. Ship the Room schema migration (add nullable `expiryDate` column) with its migration test.
2. Add the domain `expiryState` function and its unit tests.
3. Add the form field to Add medicine / Medicine details.
4. Add the tile and details-screen presentation.
No rollback concerns beyond a standard schema migration: the column is nullable and additive, so a future revert simply stops reading/writing it.

## Open Questions

- Whether the 30-day approaching-window should become configurable later, and if so, whether it should share a mechanism with a future refill lead-time setting.
- Whether a future refill/low-stock feature should reuse the `ExpiryState`-style three-state model for its own warning, once it exists.
