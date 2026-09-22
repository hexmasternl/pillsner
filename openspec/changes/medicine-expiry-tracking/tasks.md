## 1. Domain and data layer

- [ ] 1.1 Add a nullable `expiryDate: LocalDate?` field to the `Medication` and `NewMedication` domain models, default `null`
- [ ] 1.2 Add a nullable `expiryDate` column to the medicine Room entity
- [ ] 1.3 Write the Room migration adding the column, defaulting existing rows to `NULL`
- [ ] 1.4 Write a migration test asserting the schema change and that existing rows survive with `expiryDate = null`
- [ ] 1.5 Extend the medicine repository's read/write mapping to carry `expiryDate` through unchanged, both the `Medication` and `NewMedication` sides

## 2. Domain layer

- [ ] 2.1 Add an `ExpiryState` type (`NONE`, `APPROACHING`, `PAST`) and a pure `expiryState(expiryDate, today, approachingWindow = 30.days)` function with no Android dependency
- [ ] 2.2 Unit test `expiryState` around today/tomorrow boundaries, the 30-day window edge, month and year boundaries, and leap days
- [ ] 2.3 Add validation: expiry date, when set, must not be before "used since" (mirrors the existing use-until rule)

## 3. Add medicine form

- [ ] 3.1 Add an expiry date field with a date picker to the Add medicine form, default empty
- [ ] 3.2 Wire the expiry-not-before-used-since validation into the form's error state
- [ ] 3.3 Add a clear affordance for the expiry date field
- [ ] 3.4 Add string resources for the field label, hint and validation error
- [ ] 3.5 Include the expiry date in the atomic save write

## 4. Medicine details form

- [ ] 4.1 Reuse the Add medicine form's expiry date field and validation in the details screen
- [ ] 4.2 Pre-populate the expiry date from the loaded medicine
- [ ] 4.3 Confirm editing/clearing the expiry date does not withdraw, alter or otherwise touch any pending or recorded dose
- [ ] 4.4 Confirm the update write replaces the expiry date atomically with the rest of the medicine's fields

## 5. Medicines screen (tile heads-up)

- [ ] 5.1 Design the expiry indicator (icon/badge, copy, colour-independent distinction) with the `pillsner-designer` agent against `docs/design-system.md`
- [ ] 5.2 Add the indicator to the medicine tile composable, driven by `expiryState`
- [ ] 5.3 Ensure the indicator renders for both active and inactive tiles
- [ ] 5.4 Add the expiry state to the tile's combined screen-reader announcement
- [ ] 5.5 Add previews for tile states: no expiry, approaching, past, past + inactive

## 6. Verification

- [ ] 6.1 Run `pillsner-ui-review` against the changed composables
- [ ] 6.2 Run unit tests, Room migration tests and lint from the `src` Gradle project root
- [ ] 6.3 Manually verify the Add medicine, Medicine details and Medicines screens with TalkBack and the largest system font scale
