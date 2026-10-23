---
name: room-schema-change
description: The checklist for changing Pillsner's Room database - entity and DAO edits, bumping PillsnerDatabase.VERSION, writing the migration in Migrations.kt and adding it to Migrations.ALL, committing the exported schema JSON, and adding the migration test that proves existing data survives. Use whenever a change adds, removes or alters an @Entity, a column, an index, a foreign key or a table, and when reviewing such a change.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

People's medication history lives in this database and nowhere else — there is no cloud copy to
restore from. A broken migration loses it on the next app update. `CLAUDE.md` therefore requires
every schema change to ship with a migration and a migration test, and forbids destructive migration
outside debug builds.

## Where things are

- `src/app/src/main/java/nl/hexmaster/pillsner/data/db/PillsnerDatabase.kt` — `@Database` with the
  entity list, `exportSchema = true`, and `const val VERSION`.
- `.../data/db/Migrations.kt` — one `val MIGRATION_<from>_<to> = object : Migration(from, to)` per
  step, and `val ALL` listing them in order. The builder calls `.addMigrations(*Migrations.ALL)`.
- `src/app/schemas/nl.hexmaster.pillsner.data.db.PillsnerDatabase/<version>.json` — the exported
  schema of every version, also on the `androidTest` asset path for `MigrationTestHelper`.
- `src/app/src/androidTest/java/nl/hexmaster/pillsner/data/db/PillsnerDatabaseMigrationTest.kt` —
  one `migrate<n>To<m>_<whatItDoes>` test per migration.

## Checklist

1. **Design the change in the OpenSpec design first.** A schema change is never incidental: the
   design names the new columns or tables, their defaults for existing rows, and what the migration
   does with existing data. Existing rows need a sensible value — decide it there, not in the SQL.
2. **Edit the entities and DAOs.** The domain layer stays free of Room: map entities to domain models
   in the data layer, as the existing repositories do.
3. **Bump `PillsnerDatabase.VERSION` by exactly one** per change, even if the change alters several
   tables. One change, one version, one migration.
4. **Write the migration** as `MIGRATION_<n>_<n+1>` in `Migrations.kt`, with a KDoc saying what it
   changes and why, and append it to `ALL`. Write SQL that produces exactly what Room expects —
   copy the `createSql` and index statements from the new exported JSON rather than writing them
   from memory, because `runMigrationsAndValidate` compares column types, nullability, defaults and
   indices exactly.
   - SQLite can't drop or alter most column properties in place: for those, create the new table,
     copy the rows, drop the old one and rename, inside the migration.
   - Never use `AutoMigration` for anything that needs a value decision; the explicit migration keeps
     the decision visible.
5. **Build** (`./gradlew :app:assembleDebug` from `src/`) so Room exports `<n+1>.json`. **Commit the
   new JSON.** Never edit or regenerate an older version's JSON — those are the record of what is on
   people's phones.
6. **Write the migration test** in `PillsnerDatabaseMigrationTest`, following the existing ones:
   create the database at version `n` with `helper.createDatabase`, insert representative rows with
   raw SQL (including edge cases: nulls, the oldest row shapes, a medicine with doses and stock),
   run `helper.runMigrationsAndValidate(TEST_DATABASE, n + 1, true, Migrations.MIGRATION_<n>_<n+1>)`,
   and assert the existing data came through with the values the design promised.
7. **DAO tests** for any new or changed non-trivial query (in `data/db/*DaoTest`).
8. **Run** `PillsnerDatabaseMigrationTest` and the `data/db` tests on the emulator (the
   `emulator-tests` skill selects all of `data/db/` for a schema change). Also run the full chain
   check: the existing `version1_matchesTheExportedSchema` and every earlier migration test still pass.
9. **Check what else reads the schema**: backup and restore (`data-backup-restore`), app reset
   (`data/reset/RoomAppDataEraserTest`), and the wear sync payload if the new data is shown on the
   watch (`wear-sync-contract` skill).

## Guardrails

- Never `fallbackToDestructiveMigration` (any variant) in a release build. Never bump the version
  without a migration "because there's no data yet" — there is, on every installed phone.
- Never change a migration that has shipped (anything that reached `main`). A mistake in a shipped
  migration is fixed by a new migration, not by editing the old one.
- Never squash two changes' schema bumps into one version once either has merged to `development`.
- Never log row contents (medicine names, doses) in a migration, per `CLAUDE.md`'s privacy rules.
