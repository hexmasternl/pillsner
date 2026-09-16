## Why

The `website-single-page-hugo` change delivers a working Hugo site under `src/website/` but explicitly leaves hosting and deployment out of scope, noting it as an open question for a follow-up change. The site currently only builds locally; nothing publishes it, so there is still no public URL for anyone to visit. Azure credentials for a subscription, tenant and app registration are already available as repository secrets, so the missing piece is a GitHub Actions workflow that builds the Hugo site and publishes it to an Azure Static Web Apps (Free tier) instance whenever the site changes on `main`.

## What Changes

- Add a new GitHub Actions workflow (`.github/workflows/website.yml`) that:
  - Triggers on push to `main` when files under `src/website/**` change (plus manual `workflow_dispatch` for redeploying without a code change).
  - Checks out the repo, installs the pinned Hugo version, and runs `hugo --minify` from `src/website/` to produce the static output directory.
  - Authenticates to Azure using OpenID Connect (`azure/login`) with the existing `AZURE_WEBSITE_SUBSCRPITION_ID`, `AZURE_WEBSITE_TENANT_ID` and `AZURE_WEBSITE_CLIENT_ID` repository secrets — no client secret or long-lived deployment token is stored in GitHub.
  - Deploys the built output to an Azure Static Web Apps (Free tier) resource using that OIDC session, targeting the site's production environment.
  - Runs independently of `ci.yml`'s Android jobs: separate workflow file, no shared working directory, cache key or job dependency.
- Document the required Azure prerequisites (an existing Static Web Apps resource on the Free tier, and a federated identity credential trusting this repository's `main` branch for the app registration behind `AZURE_WEBSITE_CLIENT_ID`) as a one-time manual setup step, since Azure resource provisioning itself is out of scope for a GitHub Actions change.
- Update `README.md`'s repository layout/toolchain notes to mention that `src/website/` is deployed automatically to Azure Static Web Apps on merge to `main`.

## Capabilities

### New Capabilities
- `website-deployment`: the automated build-and-publish pipeline that takes `src/website/` content on `main` and deploys it to Azure Static Web Apps (Free tier) via GitHub Actions and OIDC.

### Modified Capabilities
- (none — this change adds a new, self-contained CI/CD capability and does not alter any existing app, spec, or Android behaviour)

## Impact

- **New code**: `.github/workflows/website.yml` — a new, isolated GitHub Actions workflow. No changes to `.github/workflows/ci.yml` or `release.yml`.
- **New infrastructure dependency**: an Azure Static Web Apps (Free tier) resource and a Microsoft Entra app registration with a federated credential for this repository/branch, both provisioned once outside this repository (Azure portal or CLI), referenced only by the three existing secrets.
- **README.md**: notes that the website is auto-deployed, consistent with the layout table already tracking `src/website/`.
- **No changes** to `src/app`, `src/wear`, `src/shared`, Room schemas, alarm/reminder scheduling, or any Android permission, and no change to the app's no-network privacy stance — this pipeline only ever touches the informational website, never the Android app's data or connectivity.
