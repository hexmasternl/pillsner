## 1. Azure identity prerequisites (manual, one-time; the workflow cannot grant itself access)

- [ ] 1.1 Confirm (or create) a Microsoft Entra app registration whose client ID matches `AZURE_WEBSITE_CLIENT_ID`, with a federated identity credential trusting GitHub OIDC tokens issued for this repository's `main` branch.
- [ ] 1.2 Confirm the app registration has a subscription-scope role assignment (e.g. `Contributor` on the subscription identified by `AZURE_WEBSITE_SUBSCRIPTION_ID`) sufficient to create a resource group and a Static Web App — required because `infra/website/main.bicep` creates the resource group itself.
- [ ] 1.3 Confirm the three repository secrets (`AZURE_WEBSITE_CLIENT_ID`, `AZURE_WEBSITE_TENANT_ID`, `AZURE_WEBSITE_SUBSCRIPTION_ID`) exist under exactly those names in GitHub Settings → Secrets and variables → Actions, and match the app registration/subscription from 1.1–1.2.

## 2. Infrastructure as code

- [x] 2.1 Create `infra/website/static-web-app.bicep`: a resource-group-scoped module that creates a `Microsoft.Web/staticSites` resource on the Free tier, with no `repositoryUrl` (content is pushed by the workflow, not by Azure's own GitHub integration), and outputs the resource name, default hostname, and a `@secure()` deployment token via `listSecrets()`.
- [x] 2.2 Create `infra/website/main.bicep`: a subscription-scoped template that creates the resource group and deploys the `static-web-app.bicep` module into it, re-exposing its outputs (including the `@secure()` deployment token).
- [x] 2.3 Validate both templates compile (`az bicep build`) with no errors.

## 3. Workflow implementation

- [x] 3.1 Create `.github/workflows/website.yml` with `on: push` to `main` path-filtered to `src/website/**`, and `on: workflow_dispatch`.
- [x] 3.2 Add checkout and Hugo setup steps pinned to the version in `README.md`'s toolchain table (extended edition), and run `hugo --minify` from `src/website/`.
- [x] 3.3 Add `permissions: id-token: write` (and `contents: read`) to the job, and an `azure/login` step using `AZURE_WEBSITE_CLIENT_ID`, `AZURE_WEBSITE_TENANT_ID`, `AZURE_WEBSITE_SUBSCRIPTION_ID`.
- [x] 3.4 Add a provisioning step that runs `az deployment sub create` against `infra/website/main.bicep`, captures the `deploymentToken` and `defaultHostname` outputs via `--query properties.outputs -o json` + `jq`, and masks the token with `::add-mask::` before exposing it as a step output.
- [x] 3.5 Add a deploy step that runs `swa deploy` against the Hugo build output, authenticated with `--deployment-token` from the provisioning step — no AAD flags, no interactive fallback possible.

## 4. Documentation

- [x] 4.1 Update `README.md`'s `.github/workflows/` row to describe `website.yml` accurately: builds and deploys the Hugo site to Azure Static Web Apps on push to `main` under `src/website/**`, and that `ci.yml`/`release.yml` skip website-only changes.
- [x] 4.2 Add a short "Deployment" note to `src/website/README.md` pointing at `.github/workflows/website.yml`, `infra/website/`, and the one remaining manual prerequisite (section 1 above).
- [ ] 4.3 Update this change's design/spec docs' secret name to the corrected `AZURE_WEBSITE_SUBSCRIPTION_ID` spelling everywhere it's referenced, once the actual GitHub secret name is confirmed (task 1.3).

## 5. Verification

- [ ] 5.1 Push a trivial change under `src/website/` to a branch, open a PR to confirm the workflow does NOT run on the PR (production-only trigger), then merge to `main` and confirm the workflow runs.
- [ ] 5.2 Confirm the first run provisions the resource group and Static Web App (visible in the Azure portal / `az staticwebapp list`), and that the deployed site is reachable at the reported default hostname.
- [ ] 5.3 Run the workflow a second time (e.g. via `workflow_dispatch`) and confirm the provisioning step succeeds as a no-op against the already-existing resources, and the site still deploys.
- [ ] 5.4 Confirm an unrelated Android-only commit to `main` does not trigger `website.yml`, and that a website-only commit does not trigger `ci.yml`/`release.yml`.
- [ ] 5.5 Confirm the deployment token does not appear in plain text anywhere in the workflow run's logs.
