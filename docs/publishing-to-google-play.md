# Publishing Pillsner to Google Play with GitHub Actions

Everything needed to get Pillsner from this repository into the Google Play Store, and to keep it
going there from a pipeline. Written for someone who has never shipped an Android app before, so it
starts at the developer account and ends at a production rollout.

Read [What you are actually publishing](#what-you-are-actually-publishing) first. The rest can be
worked through in order; each part says what it produces and what depends on it.

---

## Table of contents

1. [What you are actually publishing](#what-you-are-actually-publishing)
2. [The order of operations](#the-order-of-operations)
3. [Part 1 — The Google Play developer account](#part-1--the-google-play-developer-account)
4. [Part 2 — Create the app in the Play Console](#part-2--create-the-app-in-the-play-console)
5. [Part 3 — Signing keys](#part-3--signing-keys)
6. [Part 4 — Wire signing and versioning into Gradle](#part-4--wire-signing-and-versioning-into-gradle)
7. [Part 5 — Version codes for two modules](#part-5--version-codes-for-two-modules)
8. [Part 6 — The Play Developer API service account](#part-6--the-play-developer-api-service-account)
9. [Part 7 — GitHub secrets](#part-7--github-secrets)
10. [Part 8 — The GitHub Actions workflows](#part-8--the-github-actions-workflows)
11. [Part 9 — The first upload, by hand](#part-9--the-first-upload-by-hand)
12. [Part 10 — Store listing and graphic assets](#part-10--store-listing-and-graphic-assets)
13. [Part 11 — App content declarations](#part-11--app-content-declarations)
14. [Part 12 — Wear OS specifics](#part-12--wear-os-specifics)
15. [Part 13 — Test tracks and the road to production](#part-13--test-tracks-and-the-road-to-production)
16. [Part 14 — Release notes in the repository](#part-14--release-notes-in-the-repository)
17. [Part 15 — Troubleshooting](#part-15--troubleshooting)
18. [Appendix A — Secret and variable reference](#appendix-a--secret-and-variable-reference)
19. [Appendix B — Command reference](#appendix-b--command-reference)
20. [Keeping this document honest](#keeping-this-document-honest)

---

## What you are actually publishing

Pillsner is two Gradle application modules that share one identity:

| Module | Application id | `minSdk` | Artifact |
| --- | --- | --- | --- |
| `:app` (phone) | `nl.hexmaster.pillsner` | 26 | `src/app/build/outputs/bundle/release/app-release.aab` |
| `:wear` (watch) | `nl.hexmaster.pillsner` | 30 | `src/wear/build/outputs/bundle/release/wear-release.aab` |

Three consequences shape everything that follows.

**One Play Console app, two bundles.** Because both modules declare the same application id, they
are *one* app on Play, not two. Both bundles go into the *same release* on the *same track*. Play
works out which device gets which by looking at `android.hardware.type.watch` in the Wear manifest
and `minSdk`.

**Their version codes must differ.** Play rejects a release in which two artifacts share a version
code. [Part 5](#part-5--version-codes-for-two-modules) sets up a scheme that derives both from one
number so they cannot drift.

**They must be signed by the same certificate.** The Wearable Data Layer only connects a phone app
and a watch app whose package name *and* signing certificate match. Play App Signing gives you this
for free — one app entry means one app signing key, applied to both bundles — but only if you upload
both to the same app entry, which is the point above.

Two more things to keep in mind as you go:

- **The app has no `INTERNET` permission.** Nothing leaves the device. This makes the Data Safety
  form short and honest, and it means there is no backend to stand up. It does *not* exempt you from
  publishing a privacy policy URL — Play requires one from every app.
- **The app is a medication reminder.** That puts it under Play's health-related policies and makes
  the exact-alarm permission justifiable. Both are covered in
  [Part 11](#part-11--app-content-declarations).

---

## The order of operations

Some of these steps have long waiting periods in them. Start the slow ones early.

| # | Step | Blocks | Typical wait |
| --- | --- | --- | --- |
| 1 | Register the developer account, pass identity verification | everything | hours to a few days |
| 2 | Create the app entry in the Play Console | first upload | minutes |
| 3 | Generate the upload key | any release build | minutes |
| 4 | Wire signing and versioning into Gradle | the pipeline | minutes |
| 5 | Create the service account, grant it Play access | automated upload | up to 24h for permissions to propagate |
| 6 | Add the GitHub secrets | the pipeline | minutes |
| 7 | Write the workflows | automated release | minutes |
| 8 | Upload the first bundle by hand to internal testing | API uploads | minutes |
| 9 | Fill in the store listing and every App content form | any public track | an hour or two |
| 10 | Run a closed test — 12 testers, 14 days | production access on a new personal account | **14 days minimum** |
| 11 | Apply for production access, then roll out | — | days for review |

Step 10 is the one that catches people. If your developer account is a *personal* account created
after 13 November 2023, Google will not let you publish to production until you have run a closed
test with at least **12 testers opted in continuously for 14 days**. Organisation accounts are
exempt. Start that clock as soon as the app is installable.

---

## Part 1 — The Google Play developer account

**Produces:** a Play Console account that can publish apps.

1. Go to <https://play.google.com/console/signup> and sign in with the Google account that should
   own the app. Choose this deliberately: transferring an app to a different account later is a
   support ticket, not a setting.
2. Choose the account type.
   - **Personal.** Cheaper to start, but subject to the 12-tester / 14-day closed-test requirement
     before production access, and your personal name and address appear on the store listing.
   - **Organisation.** Requires a D-U-N-S number for your legal entity (free, but can take up to two
     weeks to obtain from Dun & Bradstreet). Exempt from the closed-test requirement. The
     organisation name appears on the listing.

   For Pillsner published under Hexmaster, an organisation account is the better fit — but get the
   D-U-N-S number started immediately, because it is the long pole.
3. Pay the **one-off USD 25 registration fee**.
4. Complete **identity verification**: a government ID, and for organisations the D-U-N-S number plus
   a verifiable website and phone number. Google may take a few days.
5. Under **Setup → Developer account → Developer page**, set the public developer name, the support
   email address and the physical address that will be shown on the store listing. This address is
   public. Use a business address, not a home one.
6. If you will ever charge for the app or sell anything in it, set up a **payments profile** as well.
   Pillsner is free with no in-app purchases, so this can be skipped.

---

## Part 2 — Create the app in the Play Console

**Produces:** an app entry with the package name `nl.hexmaster.pillsner`, ready to receive uploads.

1. In the Play Console, choose **All apps → Create app**.
2. Fill in:
   - **App name** — `Pillsner`. This is what users see in search results; it may be up to 30
     characters and you can localise it later.
   - **Default language** — pick the one most of your users read. The app ships English and Dutch
     (`resourceConfigurations += listOf("en", "nl")`), so choose `English (United States)` as the
     default and add `Dutch (Netherlands)` as a translation in [Part 10](#part-10--store-listing-and-graphic-assets).
   - **App or game** — App.
   - **Free or paid** — Free. Note this cannot be changed from free to paid after publishing.
3. Tick the declarations about Developer Program Policies and US export law.
4. Press **Create app**.

The package name is *not* set here. It is fixed by the first bundle you upload, and it is permanent
for the life of the app entry. Get `nl.hexmaster.pillsner` right the first time — a typo means a new
app entry and a new listing.

---

## Part 3 — Signing keys

**Produces:** an upload keystore, plus the two passwords and the alias you will store as GitHub
secrets.

### How Play App Signing works

There are two keys, and confusing them is the most common source of trouble.

```
            you                      Google Play                     devices
   ┌──────────────────┐      ┌────────────────────────┐      ┌──────────────────┐
   │  upload key      │ ───► │  verifies the upload,  │ ───► │  app signing key │
   │  (in your CI)    │      │  strips your signature │      │  (Google holds)  │
   └──────────────────┘      └────────────────────────┘      └──────────────────┘
```

- The **upload key** is yours. Your CI signs every bundle with it. Its only job is to prove to Google
  that the upload came from you. If you lose it, you ask Google to reset it and carry on.
- The **app signing key** is what devices actually verify. With Play App Signing enabled — which is
  mandatory for new apps — Google generates and holds it. You can never lose it, and it is the same
  key for the phone bundle and the Wear bundle, which is exactly what the Data Layer needs.

So: generate an upload key, and let Google do the rest.

### Generate the upload keystore

Run this once, on your machine, **outside** the repository working tree. `keytool` ships with the
JDK 21 you already have.

```powershell
# PowerShell, from a folder you keep backed up and out of git
./keytool -genkeypair -v -keystore pillsner-upload.jks -storetype PKCS12 -keyalg RSA -keysize 4096 -validity 10000 -alias pillsner-upload -dname "CN=Pillsner, O=Hexmaster, L=Utrecht, C=NL"
```

It prompts for a keystore password and a key password. Use a password manager and generate two long
random ones — or use the same value for both, which is what most CI setups do and what the Gradle
snippet below assumes is *allowed* but not required.

Notes on the flags:

- `-storetype PKCS12` is the modern format. The old `JKS` type still works but `keytool` will nag.
- `-keysize 4096` is comfortably above Play's 2048-bit minimum.
- `-validity 10000` is about 27 years. Play requires the key to remain valid until at least
  22 October 2033; 10000 days from today clears that easily.
- `-dname` avoids the interactive questionnaire. The values are cosmetic — they end up in the
  certificate, not in the store listing.

### Back it up

Three copies, at least one offline, none of them in the repository. The `.gitignore` in `src/`
already refuses `*.jks`, `*.keystore`, `keystore.properties` and `signing.properties`, so an
accidental `git add` is unlikely — but do not rely on that as your backup strategy.

### Record the four values

You will need these in [Part 7](#part-7--github-secrets):

| Value | From |
| --- | --- |
| The keystore file itself | `pillsner-upload.jks` |
| Keystore password | what you typed at the first prompt |
| Key alias | `pillsner-upload` |
| Key password | what you typed at the second prompt |

### Local signing (optional but handy)

To produce a signed release build on your own machine without setting environment variables, create
`src/keystore.properties` — already gitignored:

```properties
storeFile=C:/keys/pillsner-upload.jks
storePassword=...
keyAlias=pillsner-upload
keyPassword=...
```

The Gradle code in the next part reads this file when it exists and falls back to environment
variables when it does not, so the same build script serves your laptop and the pipeline.

---

## Part 4 — Wire signing and versioning into Gradle

**Produces:** `assembleRelease` and `bundleRelease` that sign with your upload key and take their
version from the build environment.

Right now `src/app/build.gradle.kts` and `src/wear/build.gradle.kts` have a hard-coded
`versionCode = 1` / `versionName = "0.1.0"` and no `signingConfigs` block at all, so a release build
comes out unsigned. Both modules need the same treatment.

### The shared block

Add this to the **top** of `src/app/build.gradle.kts`, after the existing `import` line and before
`plugins { ... }`:

```kotlin
import java.util.Properties
```

…and this immediately after the `plugins { ... }` block:

```kotlin
// Signing material comes from src/keystore.properties on a developer machine and from environment
// variables in CI. Neither is committed; see docs/publishing-to-google-play.md.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(property: String, environmentVariable: String): String? =
    keystoreProperties.getProperty(property)
        ?: providers.environmentVariable(environmentVariable).orNull

val keystorePath: String? = signingValue("storeFile", "PILLSNER_KEYSTORE_PATH")

// One number decides the version code of both applications; see Part 5 of the publishing guide.
val versionCodeBase: Int =
    providers.environmentVariable("PILLSNER_VERSION_CODE").orNull?.toInt() ?: 1
val releaseVersionName: String =
    providers.environmentVariable("PILLSNER_VERSION_NAME").orNull ?: "0.1.0"
```

`providers.environmentVariable(...)` rather than `System.getenv(...)` is deliberate: the project runs
with `org.gradle.configuration-cache=true`, and the provider API registers the variable as a proper
configuration-cache input instead of tripping a cache miss or an "undeclared build input" warning.

### Inside `android { }` in `src/app/build.gradle.kts`

Replace the two version lines in `defaultConfig`:

```kotlin
    defaultConfig {
        applicationId = "nl.hexmaster.pillsner"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        // The phone application takes the even slot; the watch takes the one above it.
        versionCode = versionCodeBase * 10
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("en", "nl")
    }
```

Add a `signingConfigs` block (anywhere inside `android { }`, conventionally just before
`buildTypes`):

```kotlin
    signingConfigs {
        // Only created when signing material is present, so a plain local `bundleRelease`
        // still produces an (unsigned) artifact instead of failing the configuration phase.
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = signingValue("storePassword", "PILLSNER_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "PILLSNER_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "PILLSNER_KEY_PASSWORD")
            }
        }
    }
```

And point the release build type at it:

```kotlin
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
```

### The same in `src/wear/build.gradle.kts`

Identical, with one character different — the version code:

```kotlin
        // One above the phone's, so the two artifacts in a release never collide.
        versionCode = versionCodeBase * 10 + 1
        versionName = releaseVersionName
```

Duplicating twenty lines across two modules is mildly annoying. If it starts to bother you, the
clean fix is a convention plugin in `src/build-logic`, applied by both modules — but that is a
change proposal of its own, not something to bolt on here.

### Verify it locally

From `src`:

```powershell
.\gradlew.bat :app:bundleRelease :wear:bundleRelease
```

Then confirm the artifacts exist and are signed by your key:

```powershell
# The bundles
Get-ChildItem app\build\outputs\bundle\release\app-release.aab
Get-ChildItem wear\build\outputs\bundle\release\wear-release.aab

# The signature — the SHA-256 shown here must match on both
jarsigner -verify -verbose:summary -certs app\build\outputs\bundle\release\app-release.aab
```

Two things worth knowing about the bundle:

- **The R8 mapping file is inside it.** Because `isMinifyEnabled = true`, AGP packs
  `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map` into the AAB and Play picks it
  up automatically. There is no separate mapping upload step, and crash reports in the Play Console
  will be deobfuscated.
- **Both languages ship in the base install.** Both modules already set
  `bundle { language { enableSplit = false } }`, which is what makes the in-app language picker work
  for a user whose phone is set to neither English nor Dutch.

---

## Part 5 — Version codes for two modules

Play enforces three rules, and the scheme above satisfies all of them:

1. Every artifact in a release has a **unique** version code. Phone gets `base * 10`, watch gets
   `base * 10 + 1`.
2. A new release must have a **higher** version code than any release before it, on any track — even
   a release you later halted. Version codes are spent, never reused.
3. The version code is an integer below 2,100,000,000.

Derive `base` from the git tag so it is monotonic and traceable. For a tag `v1.2.3`:

```
base = major * 10000 + minor * 100 + patch
     = 1 * 10000 + 2 * 100 + 3
     = 10203

phone versionCode = 102030
watch versionCode = 102031
versionName       = "1.2.3"
```

This survives up to 99 patches and 99 minors per major, and leaves eight spare slots per release in
case a third form factor ever appears. The release workflow computes it from the tag, so the only
thing you ever decide by hand is the tag itself.

Do **not** derive the version code from `github.run_number`. It resets if the workflow file is
renamed, and a version code that goes backwards is a release you cannot publish.

---

## Part 6 — The Play Developer API service account

**Produces:** a JSON key that lets GitHub Actions upload bundles and manage tracks on your behalf.

This is the fiddliest part, because it spans two consoles. Do it in this order.

### 6.1 Link a Google Cloud project

1. Play Console → **Setup → API access**.
2. If no project is linked, either **Create new project** (Google makes one for you) or
   **Link existing project** if you already have a Google Cloud project you want to use.
3. Accept the terms. The page now shows the linked project and a service accounts section.

### 6.2 Enable the API and create the service account

1. From that same page, follow the link into the **Google Cloud Console**.
2. **APIs & Services → Library**, search for **Google Play Android Developer API**, and press
   **Enable**. Without this the pipeline fails with a `403 ... has not been used in project ...`
   error.
3. **IAM & Admin → Service accounts → Create service account**.
   - **Name**: `pillsner-play-publisher`
   - **Description**: `Uploads Pillsner bundles to Google Play from GitHub Actions`
   - Skip the "Grant this service account access to project" step — it needs **no** Google Cloud IAM
     roles. Its permissions live in the Play Console, not in GCP.
4. Open the new service account → **Keys → Add key → Create new key → JSON**. A `.json` file
   downloads. **This is the only copy.** Treat it like the keystore: it is a credential that can
   publish to your store listing.

### 6.3 Grant it access in the Play Console

Back in the Play Console:

1. **Users and permissions → Invite new users**.
2. Paste the service account's email address — it looks like
   `pillsner-play-publisher@<project-id>.iam.gserviceaccount.com` and is in the JSON file under
   `client_email`.
3. Under **App permissions**, add **Pillsner** and grant exactly these, and nothing more:

   | Permission | Why |
   | --- | --- |
   | View app information and download bulk reports | The API reads the current track state before editing it |
   | Release to testing tracks | Uploads to `internal`, `alpha`, `beta` |
   | Manage testing track releases | Creates and edits those releases |
   | Release app to production, exclude devices, and use app signing | Only if the pipeline should publish to production directly |
   | Manage store presence | Only if the pipeline should push release notes or listing text |

   Leave **Admin (all permissions)** unticked. A leaked key should not be able to transfer your app.
4. **Invite user**.

Permission changes can take **up to 24 hours** to propagate to the API. If your first pipeline run
fails with `The current user has insufficient permissions to perform the requested operation`, wait
and try again before assuming you configured it wrong.

### 6.4 Sanity-check the key

Before wiring it into CI, confirm the credential works. Save this as `check-play-access.py` next to
the JSON file, then run `pip install google-api-python-client google-auth` and
`python check-play-access.py`:

```python
from google.oauth2 import service_account
from googleapiclient.discovery import build

PACKAGE = "nl.hexmaster.pillsner"

credentials = service_account.Credentials.from_service_account_file(
    "pillsner-play-publisher.json",
    scopes=["https://www.googleapis.com/auth/androidpublisher"],
)
service = build("androidpublisher", "v3", credentials=credentials)

edit = service.edits().insert(body={}, packageName=PACKAGE).execute()
service.edits().delete(packageName=PACKAGE, editId=edit["id"]).execute()
print("credential works")
```

If that prints `credential works`, the pipeline will work. A `403` means revisit 6.2 and 6.3. A `404`
on the package name means the app entry does not exist yet, or has never received an upload — see
[Part 9](#part-9--the-first-upload-by-hand).

---

## Part 7 — GitHub secrets

**Produces:** the five secrets the release workflow reads.

### Encode the keystore

GitHub secrets hold text, so the binary keystore goes in base64:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\keys\pillsner-upload.jks")) | Set-Clipboard
```

### Add them

Repository → **Settings → Secrets and variables → Actions → New repository secret**, five times:

| Secret name | Value |
| --- | --- |
| `PLAY_UPLOAD_KEYSTORE_BASE64` | the base64 string just copied |
| `PLAY_UPLOAD_KEYSTORE_PASSWORD` | the keystore password |
| `PLAY_UPLOAD_KEY_ALIAS` | `pillsner-upload` |
| `PLAY_UPLOAD_KEY_PASSWORD` | the key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | the **entire contents** of the service account `.json` file, verbatim |

Or, with the GitHub CLI:

```powershell
gh secret set PLAY_UPLOAD_KEYSTORE_BASE64 --body (Get-Clipboard)
gh secret set PLAY_UPLOAD_KEYSTORE_PASSWORD
gh secret set PLAY_UPLOAD_KEY_ALIAS --body "pillsner-upload"
gh secret set PLAY_UPLOAD_KEY_PASSWORD
gh secret set PLAY_SERVICE_ACCOUNT_JSON --body (Get-Content C:\keys\pillsner-play-publisher.json -Raw)
```

### Protect the production path

Create a **GitHub Environment** so a human confirms anything going to real users:

1. **Settings → Environments → New environment**, name it `google-play`.
2. Add yourself under **Required reviewers**.
3. Optionally move the five secrets from repository scope into this environment, so only jobs that
   declare `environment: google-play` can read them.

The release workflow below declares that environment, so every run pauses for approval before it
touches Play.

---

## Part 8 — The GitHub Actions workflows

**Produces:** `.github/workflows/ci.yml` and `.github/workflows/release.yml`.

Two workflows, cleanly separated: one that runs on every change and never touches Play, and one that
runs on a tag and does.

Note that the Gradle project root is `src`, not the repository root. The `run` steps handle that with
`defaults.run.working-directory`, but **`uses:` steps ignore that setting** — any path passed to an
action must be written relative to the repository root. This trips people up constantly; the upload
step below is written accordingly.

### 8.1 `.github/workflows/ci.yml`

```yaml
name: CI

on:
  pull_request:
  push:
    branches: [ main ]

permissions:
  contents: read

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    name: Test, lint and assemble
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: src

    steps:
      - name: Check out
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"

      - name: Validate the Gradle wrapper
        uses: gradle/actions/wrapper-validation@v4

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          cache-read-only: ${{ github.ref != 'refs/heads/main' }}

      - name: Set up the Android SDK
        uses: android-actions/setup-android@v3

      - name: Install the pinned SDK components
        run: sdkmanager "platforms;android-37" "build-tools;36.0.0"

      - name: Unit tests
        run: ./gradlew test

      - name: Lint
        run: ./gradlew lint

      - name: Assemble debug
        run: ./gradlew assembleDebug

      - name: Upload reports on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: reports
          path: |
            src/**/build/reports/**
            src/**/build/test-results/**
          retention-days: 7
```

`CLAUDE.md` asks that unit tests and lint pass before any task is declared done — this is that rule,
enforced. Instrumented tests are deliberately *not* here: they need an emulator, which roughly
quadruples the run time. Add a separate scheduled workflow using
`reactivecircus/android-emulator-runner` if you want `connectedAndroidTest` on a cadence — the
alarm-scheduling and Room migration tests are the ones that earn the wait.

### 8.2 `.github/workflows/release.yml`

```yaml
name: Release to Google Play

on:
  push:
    tags: [ "v*.*.*" ]
  workflow_dispatch:
    inputs:
      track:
        description: Play track to publish to
        type: choice
        options: [ internal, alpha, beta, production ]
        default: internal
      version:
        description: Version, without the leading v (for example 1.2.3)
        required: true

permissions:
  contents: read

concurrency:
  group: release
  cancel-in-progress: false

jobs:
  publish:
    name: Build, sign and upload
    runs-on: ubuntu-latest
    environment: google-play          # pauses here for the reviewer configured in Part 7
    defaults:
      run:
        working-directory: src

    steps:
      - name: Check out
        uses: actions/checkout@v4

      - name: Work out the version
        id: version
        working-directory: .
        env:
          EVENT_NAME: ${{ github.event_name }}
          INPUT_VERSION: ${{ inputs.version }}
          INPUT_TRACK: ${{ inputs.track }}
        run: |
          if [ "$EVENT_NAME" = "workflow_dispatch" ]; then
            VERSION="$INPUT_VERSION"
            TRACK="$INPUT_TRACK"
          else
            VERSION="${GITHUB_REF_NAME#v}"
            TRACK="internal"
          fi
          case "$VERSION" in
            [0-9]*.[0-9]*.[0-9]*) ;;
            *) echo "::error::Version $VERSION is not major.minor.patch" ; exit 1 ;;
          esac
          MAJOR=$(echo "$VERSION" | cut -d. -f1)
          MINOR=$(echo "$VERSION" | cut -d. -f2)
          PATCH=$(echo "$VERSION" | cut -d. -f3)
          BASE=$(( MAJOR * 10000 + MINOR * 100 + PATCH ))
          {
            echo "name=$VERSION"
            echo "code=$BASE"
            echo "track=$TRACK"
          } >> "$GITHUB_OUTPUT"
          echo "Publishing $VERSION (base $BASE) to $TRACK" >> "$GITHUB_STEP_SUMMARY"

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"

      - name: Validate the Gradle wrapper
        uses: gradle/actions/wrapper-validation@v4

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Set up the Android SDK
        uses: android-actions/setup-android@v3

      - name: Install the pinned SDK components
        run: sdkmanager "platforms;android-37" "build-tools;36.0.0"

      - name: Unit tests
        run: ./gradlew test

      - name: Lint
        run: ./gradlew lint

      - name: Restore the upload keystore
        env:
          KEYSTORE_BASE64: ${{ secrets.PLAY_UPLOAD_KEYSTORE_BASE64 }}
        run: |
          echo "$KEYSTORE_BASE64" | base64 -d > "$RUNNER_TEMP/upload.jks"
          test -s "$RUNNER_TEMP/upload.jks"

      - name: Build both bundles
        env:
          PILLSNER_KEYSTORE_PATH: ${{ runner.temp }}/upload.jks
          PILLSNER_KEYSTORE_PASSWORD: ${{ secrets.PLAY_UPLOAD_KEYSTORE_PASSWORD }}
          PILLSNER_KEY_ALIAS: ${{ secrets.PLAY_UPLOAD_KEY_ALIAS }}
          PILLSNER_KEY_PASSWORD: ${{ secrets.PLAY_UPLOAD_KEY_PASSWORD }}
          PILLSNER_VERSION_CODE: ${{ steps.version.outputs.code }}
          PILLSNER_VERSION_NAME: ${{ steps.version.outputs.name }}
        run: ./gradlew :app:bundleRelease :wear:bundleRelease

      - name: Confirm both bundles are signed
        run: |
          jarsigner -verify app/build/outputs/bundle/release/app-release.aab
          jarsigner -verify wear/build/outputs/bundle/release/wear-release.aab

      - name: Upload to Google Play
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.PLAY_SERVICE_ACCOUNT_JSON }}
          packageName: nl.hexmaster.pillsner
          # Paths are relative to the repository root: uses-steps ignore defaults.run
          releaseFiles: |
            src/app/build/outputs/bundle/release/app-release.aab
            src/wear/build/outputs/bundle/release/wear-release.aab
          track: ${{ steps.version.outputs.track }}
          status: completed
          whatsNewDirectory: distribution/whatsnew
          changesNotSentForReview: false

      - name: Keep the bundles as build artifacts
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: pillsner-${{ steps.version.outputs.name }}
          path: |
            src/app/build/outputs/bundle/release/app-release.aab
            src/wear/build/outputs/bundle/release/wear-release.aab
          retention-days: 90
```

### 8.3 Things worth knowing about that workflow

- **Both bundles go up in one call.** `releaseFiles` takes a list, and the action puts every file in
  a single Play release. Uploading them in two separate steps would create two releases, the second
  of which supersedes the first — and the watch app would quietly vanish.
- **Secrets are passed through `env:`, never interpolated into a `run:` script.** A secret spliced
  directly into shell text is a script-injection hole and can break the script outright if the value
  contains a quote.
- **`r0adkll/upload-google-play` is a third-party action** holding your publishing credential. Pin it
  to a commit SHA rather than `@v1` once you have settled on a version:
  `uses: r0adkll/upload-google-play@<40-char-sha>`. The alternative is Fastlane's `supply`, which is
  more configurable at the cost of a Ruby toolchain in CI.
- **No mapping file is uploaded.** It is already inside the AAB — see Part 4.
- **`status: completed`** publishes the release immediately on that track. Use `status: draft` while
  you are still getting the workflow right, then inspect the draft in the Play Console before
  promoting anything. For a staged production rollout, use `status: inProgress` together with
  `userFraction: 0.1`.
- **Tag pushes always go to `internal`.** Production is reachable only through a deliberate
  `workflow_dispatch` with `track: production`, which also passes through the environment approval.
  Make the dangerous thing require a decision.

### 8.4 Cutting a release

```powershell
git tag -a v1.0.0 -m "Pillsner 1.0.0"
git push origin v1.0.0
```

The workflow runs, waits for your approval, and the build appears on the internal track within a few
minutes. Widening the audience is then a button in the Play Console, or another `workflow_dispatch`.

---

## Part 9 — The first upload, by hand

**Produces:** a package name registered against the app entry, so the API will accept everything
afterwards.

The Play Developer API can only work on an app that already exists *and* already has a package name
bound to it. Until the first bundle lands, API calls return `404` for
`nl.hexmaster.pillsner`. So the very first one goes up through the browser.

1. Build both bundles locally, signed, using `src/keystore.properties` from Part 3:

   ```powershell
   cd src
   .\gradlew.bat :app:bundleRelease :wear:bundleRelease
   ```

2. Play Console → **Test and release → Testing → Internal testing → Create new release**.
3. The first time, Play asks how you want to sign. Choose **Use Google-generated key** — this is Play
   App Signing, and it is what makes the phone and watch bundles share one certificate.
4. Upload **both** files into the same release:
   - `src/app/build/outputs/bundle/release/app-release.aab`
   - `src/wear/build/outputs/bundle/release/wear-release.aab`
5. Give the release a name (it defaults to the version name) and some release notes.
6. **Save**, then **Review release**, then **Start rollout to Internal testing**.

Play will list warnings on that screen. Read them; most are informational, but the ones about
missing App content declarations will block the rollout until you have done
[Part 11](#part-11--app-content-declarations).

Once this has succeeded once, the pipeline from [Part 8](#part-8--the-github-actions-workflows)
takes over and you should not need to touch the browser to ship again.

### Add yourself as an internal tester

**Internal testing → Testers → Create email list**, add your own address, save. The opt-in link on
that page is what installs the app on your phone. Wear OS installs follow the phone install
automatically once the watch is paired.

---

## Part 10 — Store listing and graphic assets

**Produces:** everything the store page shows. None of this is in the repository, so keep the source
files somewhere you will find them again — `distribution/` in this repo is a reasonable home, since
the release workflow already reads release notes from there.

### Text

Play Console → **Grow users → Store presence → Main store listing**.

| Field | Limit | Notes |
| --- | --- | --- |
| App name | 30 chars | `Pillsner` |
| Short description | 80 chars | Shown first. Lead with the promise. |
| Full description | 4000 chars | Plain text, light formatting. Keyword-stuffing is a policy violation. |

Something close to the product promise, for the English listing:

> **Short:** Medication reminders that arrive on time and confirm in one tap.
>
> **Full:** Pillsner reminds you to take your medication and records whether you did. Add a
> medicine, set when it is due, and a reminder arrives at exactly that moment — confirm, snooze or
> skip straight from the notification, or from your watch. Everything stays on your phone: Pillsner
> has no account, no internet permission and no analytics, so your medication list never leaves the
> device.

Then **Add translations → Dutch (Netherlands)** and write the same two fields in Dutch. The app ships
`values` and `values-nl`, so a Dutch-language listing matches what a Dutch user will actually see.

### Graphics

| Asset | Spec | Required |
| --- | --- | --- |
| App icon | 512 × 512 PNG, 32-bit, under 1 MB | Yes |
| Feature graphic | 1024 × 500 PNG or JPEG, no alpha | Yes |
| Phone screenshots | 2 to 8, PNG or JPEG, 320–3840 px on each side, aspect ratio at most 2:1 | Yes, at least 2 |
| Tablet screenshots (7" and 10") | same rules | Only if you claim tablet support |
| Wear OS screenshots | 1 to 8, 384 × 384 | Yes, if the Wear form factor is listed |
| Promo video | YouTube URL | No |

The design system is the source for all of it: Pillsner green and blue, white ground, red reserved
for danger, Raleway 48/200 for a screen title and Montserrat 18/400 for body text. See
`docs/design-system.md` and its visual companion `docs/design-system.html`.

Capture screenshots from a real device or a clean emulator with plausible but fictional medication
data. Two rules that are easy to break by accident:

- **No real medical data and no real person's name** in a screenshot. It is a public store page.
- **No device frames, mock status bars or marketing text baked into the image** that misrepresents
  the app. Play rejects screenshots that show functionality the app does not have.

Good screens to show: the home screen with a dose due, the confirmation tap, today's list, adding a
medicine, and the watch tile.

### Store settings

**Grow users → Store presence → Store settings**:

- **App category** — `Medical`. `Health & Fitness` is the alternative; `Medical` is the more honest
  fit for a medication reminder and sets user expectations correctly.
- **Tags** — pick the medication/reminder ones offered.
- **Contact details** — the support email is mandatory and is shown publicly. A website and phone
  number are optional.
- **External marketing** — leave the default unless you have a reason.

---

## Part 11 — App content declarations

**Produces:** a green checklist under **Policy → App content**. Every item must be complete before
any track other than internal testing will publish.

Work through them in this order.

### 11.1 Privacy policy

Mandatory for every app, including one with no internet permission. It must be a public URL that
loads without a login and is not a PDF.

Pillsner already has the text in the app — see `src/app/src/main/java/nl/hexmaster/pillsner/domain/legal/`
and the disclaimer and terms strings. The cheapest hosting that stays in step with the repository is
GitHub Pages:

1. Add `docs/privacy-policy.md` with the same text the app shows.
2. Repository **Settings → Pages → Source: Deploy from a branch**, branch `main`, folder `/docs`.
3. The URL becomes `https://hexmasternl.github.io/pillsner/privacy-policy`.

The policy must name the developer, say what data is collected (for Pillsner: none leaves the
device), say how it is stored and how a user deletes it, and give a contact address. If you ever add
a network feature, this page and the Data safety form both have to change in the same release.

### 11.2 App access

Play asks whether any part of the app is behind a login or otherwise restricted.

Pillsner answers **All functionality is available without special access**. One caveat worth knowing:
the app offers a biometric app lock (`androidx.biometric`). A *fresh install* has no lock set, so a
reviewer gets straight in — which is why the honest answer is still "no special access". If you ever
make the lock default-on, you must supply reviewer instructions here or reviews will fail.

### 11.3 Ads

**No, my app does not contain ads.** There is no ad SDK in the dependency list and adding one would
need a change proposal.

### 11.4 Content rating

A questionnaire that produces IARC ratings for every region. Answer it honestly; a wrong answer here
is grounds for removal.

- Category: **Utility, Productivity, Communication or Other**.
- Expect questions about violence, sexuality, language, controlled substances and gambling. Pillsner
  is `No` to essentially all of them.
- There is a question about references to drugs. A medication reminder references medicines the user
  enters, not recreational drugs — answer in the spirit of the question and add a note if the form
  offers a free-text field.

The result should be PEGI 3 / ESRB Everyone.

### 11.5 Target audience and content

- **Target age groups**: 18 and over. Do not tick any under-18 group. A medication app aimed at
  children pulls in the Families policy, Designed for Families requirements, and a much stricter
  review.
- **Appeal to children**: No.

### 11.6 Data safety

The longest form, and the easiest one for Pillsner.

- *Does your app collect or share any of the required user data types?* → **No**.

  Data that stays on the device and is never transmitted is not "collected" in Play's definition.
  Pillsner has no `INTERNET` permission at all — confirm this for yourself in the merged manifest
  before you answer, because the answer is a legal declaration:

  ```powershell
  cd src
  .\gradlew.bat :app:processReleaseMainManifest
  Select-String -Path app\build\intermediates\merged_manifest\release\*\AndroidManifest.xml -Pattern "uses-permission"
  ```

- *Is all of the user data encrypted in transit?* → not asked, since nothing is in transit.
- *Do you provide a way for users to request that their data is deleted?* → **Yes**, if the app lets a
  user delete a medicine and its history; uninstalling also removes everything.

Redo this form whenever a change adds a dependency that could phone home. It is re-reviewed on every
submission.

### 11.7 Health apps declaration

Play has a **Health apps** section under App content for apps that provide health functions.
Medication management is explicitly named in the Health policy, so this applies.

Expect to declare:

- The health category: **medication management / medication reminders**.
- That the app does not provide medical diagnosis, dosage advice or treatment recommendations —
  Pillsner reminds and records; it does not advise.
- Any regulatory approvals, of which there are none, and none are required for a reminder app that
  makes no medical claims.

Keep the store listing consistent with this. Do not write copy that implies clinical outcomes
("improves adherence by 40%") unless you can evidence it — that is the fastest way to a rejection.

### 11.8 Sensitive permissions: exact alarms

The manifest declares `USE_EXACT_ALARM` (and `SCHEDULE_EXACT_ALARM` up to API 32).

`USE_EXACT_ALARM` is a restricted permission: Play grants it only to apps whose *core* function is an
alarm, timer, calendar or reminder. Pillsner is exactly that, so the declaration is straightforward,
but be ready to argue it:

- Make sure the store listing describes reminders in the first sentence. Reviewers read it.
- If the Console shows an exact-alarm declaration form, explain that the app is a medication reminder
  and that an inexact alarm would deliver a dose reminder at the wrong time.
- Never swap exact alarms for `WorkManager` to sidestep the review. `CLAUDE.md` forbids it, and it
  would break the product promise.

### 11.9 Advertising ID

`play-services-wearable` can merge `com.google.android.gms.permission.AD_ID` into the manifest
depending on its version. If that permission is present, Play requires you to declare that the app
uses an advertising ID — which Pillsner does not.

Check:

```powershell
cd src
.\gradlew.bat :app:processReleaseMainManifest
Select-String -Path app\build\intermediates\merged_manifest\release\*\AndroidManifest.xml -Pattern "AD_ID"
```

If it is there, remove it rather than declaring it. In `src/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="com.google.android.gms.permission.AD_ID"
        tools:node="remove" />
```

Then re-run the check and answer **No** on the Advertising ID form. Do the same in the Wear module.

### 11.10 The rest

| Form | Pillsner answer |
| --- | --- |
| Government apps | No |
| Financial features | None |
| News apps | No |
| Data deletion (account deletion URL) | Not applicable — there are no accounts |
| COVID-19 contact tracing | No |

---

## Part 12 — Wear OS specifics

**Produces:** a listing that offers the watch app, and a release that actually delivers it.

1. **Declare the form factor.** Play Console → **Grow users → Store presence → Store listings →
   Form factors → Wear OS → Add Wear OS**. This asks for Wear-specific screenshots (384 × 384) and
   puts the app through an extra review against the Wear OS app quality guidelines.
2. **The listing will say the app needs a phone.** The Wear manifest sets
   `com.google.android.wearable.standalone` to `false`, which is correct — the watch app shows what
   the phone sends and cannot work alone. Play surfaces this as "Requires a companion phone app".
3. **Both bundles, one release, always.** If you ever publish a phone release without the Wear bundle,
   watch users lose the app at the next update. The workflow in Part 8 always builds both.
4. **The Wear review is separate and can fail on its own.** A rejection of the Wear listing does not
   block the phone app; you will see it as a separate item in the Console. Common causes: screenshots
   that are not square, a watch UI that does not handle round screens, or no visible content when the
   phone app is absent.
5. **Testing.** Internal testers with a paired Wear OS device get the watch build automatically. To
   test on an emulator pair, remember that both modules must be signed by the same key — debug builds
   already are, via the shared debug keystore.

---

## Part 13 — Test tracks and the road to production

| Track | Audience | Review | Use it for |
| --- | --- | --- | --- |
| Internal testing | up to 100 testers by email | minimal, minutes | Every pipeline build |
| Closed testing | testers by email list or Google Group | full review | The 14-day requirement; real-world alarm testing |
| Open testing | anyone who opts in, optionally capped | full review | A public beta |
| Production | everyone | full review | Releases |

### The 12-tester, 14-day requirement

If your developer account is a **personal** account created after 13 November 2023, Play will not
grant production access until you have run a **closed test with at least 12 testers who stayed
opted in for 14 continuous days**. Then you apply for production access and Google reviews the
application.

Practical advice:

- Recruit 14 or 15 people, not 12. Someone always uninstalls.
- Testers must *opt in through the link and keep the app installed*. Opting out resets their
  contribution.
- Start on day one. This is the longest pole in the whole process, and it runs in parallel with
  everything else in this document.

An **organisation** account is exempt. If you have a legal entity and a D-U-N-S number, that route is
two weeks of paperwork instead of two weeks of waiting plus recruiting.

### The pre-launch report

Every upload to a testing track gets crawled automatically on real devices. **Test and release →
Pre-launch report** shows crashes, ANRs, accessibility findings and screenshots per device. It is
free and it catches things emulator testing does not — particularly on the accessibility side, which
matters for an app whose confirmation tap has to work with TalkBack and large fonts.

### Rolling out

- Use **managed publishing** (Setup → Managed publishing) if you want review to finish but the
  release to go live at a moment you choose.
- Use a **staged rollout** for production: start at 10 %, watch the crash rate and the vitals for a
  day, then widen. In the workflow, that is `status: inProgress` with `userFraction: 0.1`.
- If something is wrong, **halt the rollout**. Users already on the bad version stay on it — you
  cannot roll back — so the fix is always a new, higher version code.
- Reviews typically take a few hours to a few days. The first submission of a new app takes longest,
  and a health-related app can take longer still.

---

## Part 14 — Release notes in the repository

The release workflow reads `whatsNewDirectory: distribution/whatsnew`. Create that folder with one
plain-text file per listing language, named after the Play locale:

```
distribution/
  whatsnew/
    whatsnew-en-US
    whatsnew-nl-NL
```

Each file holds the release notes for that language, **maximum 500 characters**, plain text:

```
distribution/whatsnew/whatsnew-en-US
------------------------------------
Medication usage history: see what you took, and when.
Fixes for reminders arriving late after a time zone change.
```

Keeping these in the repository means the release notes are reviewed in the same pull request as the
change they describe, and the pipeline never has to ask a human what changed. Update them as part of
the change, not as an afterthought at tag time.

---

## Part 15 — Troubleshooting

### Signing

| Message | Cause and fix |
| --- | --- |
| `You uploaded an APK or Android App Bundle that is not signed with the upload certificate` | The CI keystore is not the one registered with Play. Compare fingerprints: `keytool -list -v -keystore upload.jks` against Play Console → Setup → App integrity → Upload key certificate. If you genuinely lost the key, request an upload key reset there. |
| `The Android App Bundle was not signed` | `signingConfigs.findByName("release")` returned null, so the release build type had no config. `PILLSNER_KEYSTORE_PATH` was unset or empty — check the "Restore the upload keystore" step actually produced a non-empty file. |
| `Failed to read key ... from store: Cannot recover key` | The key password is wrong (it is not always the same as the keystore password). |
| `base64: invalid input` | The secret picked up CRLF line breaks. Re-create it with `[Convert]::ToBase64String(...)` as shown in Part 7, which produces one unbroken line, and decode with `base64 -d` rather than `base64 --decode -i`. |

### The Play API

| Message | Cause and fix |
| --- | --- |
| `403 ... androidpublisher.googleapis.com ... has not been used in project ... before or it is disabled` | The Google Play Android Developer API is not enabled on the linked GCP project. Part 6.2. |
| `The current user has insufficient permissions to perform the requested operation` | The service account has no app-level permission, or the grant has not propagated. Part 6.3, then wait up to 24 hours. |
| `404 ... Package not found: nl.hexmaster.pillsner` | No bundle has ever been uploaded to this app entry. Do the manual first upload, Part 9. |
| `Only releases with status draft may be created on draft app` | The app has never been published on any track. Either complete the first rollout by hand, or set `status: draft` in the workflow until it has. |
| `APK specifies a version code that has already been used` | Version codes are spent once. Tag a higher version. |
| `Version code N has already been used` on the Wear bundle only | Both modules resolved the same `versionCodeBase` but the `* 10 + 1` line is missing from `src/wear/build.gradle.kts`. |
| `Changes cannot be sent for review automatically` | Some App content form is incomplete, or another release is already in review. Finish Part 11, or set `changesNotSentForReview: true` and submit from the Console. |

### The build

| Symptom | Cause and fix |
| --- | --- |
| CI fails at configuration with `SDK location not found` | `local.properties` is correctly gitignored and absent on the runner; `android-actions/setup-android@v3` sets `ANDROID_HOME`. Make sure that step runs before any Gradle step. |
| `Failed to install the following SDK components: platforms;android-37` | Licences not accepted, or the component genuinely is not published yet. The explicit `sdkmanager` step accepts licences via the setup action. |
| Configuration cache reports `undeclared build input: environment variable` | Something is reading `System.getenv` at configuration time. Use `providers.environmentVariable(...)` as in Part 4. |
| The release build succeeds but the app crashes only in release | R8 removed something reflective. Add a keep rule to `src/app/proguard-rules.pro`, and test a release build locally before tagging: `.\gradlew.bat :app:installRelease`. |
| Room migration tests pass locally, fail in CI | Schema JSON under `src/app/schemas` was not committed. It must be, for every version. |

### Policy

| Symptom | Cause and fix |
| --- | --- |
| Rejected for exact alarm use | The listing does not make it obvious the app is a reminder app. Rewrite the short description to lead with reminders, and re-submit. Part 11.8. |
| Rejected under the Health policy | Usually copy that reads as medical advice. Remove any claim about outcomes, dosages or treatment. Part 11.7. |
| Data safety flagged as inconsistent | A dependency added a network capability. Re-run the merged-manifest check in Part 11.6 and update the form, or drop the dependency. |
| `Your app currently targets API level N` warning | `targetSdk` must stay within one year of the latest Android release. The catalog pins 37; bump it deliberately inside a change, keeping `libs.versions.toml` and the README toolchain table in step. |

---

## Appendix A — Secret and variable reference

### GitHub Actions secrets

| Name | Shape | Used by |
| --- | --- | --- |
| `PLAY_UPLOAD_KEYSTORE_BASE64` | one line of base64 | `Restore the upload keystore` |
| `PLAY_UPLOAD_KEYSTORE_PASSWORD` | text | `Build both bundles` |
| `PLAY_UPLOAD_KEY_ALIAS` | text, `pillsner-upload` | `Build both bundles` |
| `PLAY_UPLOAD_KEY_PASSWORD` | text | `Build both bundles` |
| `PLAY_SERVICE_ACCOUNT_JSON` | the whole JSON file | `Upload to Google Play` |

### Environment variables the Gradle build reads

| Variable | Default when unset | Meaning |
| --- | --- | --- |
| `PILLSNER_KEYSTORE_PATH` | none — release goes unsigned | Absolute path to the upload keystore |
| `PILLSNER_KEYSTORE_PASSWORD` | none | Keystore password |
| `PILLSNER_KEY_ALIAS` | none | Key alias inside the keystore |
| `PILLSNER_KEY_PASSWORD` | none | Key password |
| `PILLSNER_VERSION_CODE` | `1` | Version code base; phone gets `base * 10`, watch `base * 10 + 1` |
| `PILLSNER_VERSION_NAME` | `0.1.0` | The version string users see |

All six are also readable from `src/keystore.properties` (the four signing ones) for local builds.
That file is gitignored and must stay that way.

### Files that must never be committed

`*.jks`, `*.keystore`, `keystore.properties`, `signing.properties`, the service account JSON,
`local.properties`. `src/.gitignore` already covers every one of them; verify with
`git check-ignore -v <path>` if you are ever unsure.

---

## Appendix B — Command reference

All Gradle commands run from `src`.

```powershell
# Build
.\gradlew.bat assembleDebug                       # debug APK
.\gradlew.bat :app:bundleRelease                  # phone AAB
.\gradlew.bat :wear:bundleRelease                 # watch AAB
.\gradlew.bat :app:installRelease                 # install a signed release build on a device

# Verify
.\gradlew.bat test                                # unit tests, all modules
.\gradlew.bat lint                                # Android lint
.\gradlew.bat connectedAndroidTest                # instrumented tests, needs a device
.\gradlew.bat :app:processReleaseMainManifest     # then inspect the merged manifest

# Signing
keytool -list -v -keystore pillsner-upload.jks    # fingerprints of your upload key
jarsigner -verify -verbose:summary app-release.aab

# Inspect a bundle before uploading (needs bundletool)
bundletool build-apks --bundle=app-release.aab --output=app.apks --mode=universal
bundletool install-apks --apks=app.apks
```

### Play Console shortcuts

| Where | What lives there |
| --- | --- |
| Setup → App integrity | Upload key and app signing key fingerprints |
| Setup → API access | The linked GCP project and service accounts |
| Users and permissions | Granting the service account release rights |
| Policy → App content | Every declaration from Part 11 |
| Test and release → Testing → Internal testing | Where pipeline builds land |
| Test and release → Pre-launch report | Automated device testing results |
| Grow users → Store presence → Main store listing | Text, graphics, translations |

---

## Keeping this document honest

Three things in here duplicate facts that live elsewhere in the repository, and will rot if they are
not maintained together:

- The **toolchain versions** (JDK 21, SDK 37, Build Tools 36.0.0) appear in the CI workflows, in
  `src/gradle/libs.versions.toml` and in the README toolchain table. A change that bumps one bumps
  all three.
- The **privacy policy** exists as in-app text and as a hosted page. A change to one is a change to
  both, in the same release.
- The **version code scheme** is described here and implemented in two `build.gradle.kts` files.

Anything in this document that changes app behaviour — adding a dependency, changing `targetSdk`,
adding a permission, publishing a privacy policy page — goes through the OpenSpec workflow like any
other change, not straight onto `main`.
