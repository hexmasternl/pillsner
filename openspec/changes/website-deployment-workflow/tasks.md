## 1. Azure prerequisites (manual, one-time, verify before relying on the workflow)

- [ ] 1.1 Confirm (or create) an Azure Static Web Apps resource on the Free tier for the Pillsner website.
- [ ] 1.2 Confirm (or create) a Microsoft Entra app registration whose client ID matches `AZURE_WEBSITE_CLIENT_ID`, with a federated identity credential trusting GitHub OIDC tokens issued for this repository's `main` branch.
- [ ] 1.3 Confirm the app registration has an Azure RBAC role assignment scoped to (at most) the Static Web Apps resource/resource group, sufficient to deploy content (no broader subscription-level access).
- [ ] 1.4 Record the Static Web Apps resource name and resource group for use in the workflow (as plain workflow values or repository variables, per design Open Questions).

## 2. Workflow implementation

- [x] 2.1 Create `.github/workflows/website.yml` with `on: push` to `main` path-filtered to `src/website/**`, and `on: workflow_dispatch`.
- [x] 2.2 Add checkout and Hugo setup steps pinned to the version in `README.md`'s toolchain table (extended edition), and run `hugo --minify` from `src/website/`.
- [x] 2.3 Add `permissions: id-token: write` (and `contents: read`) to the job, and an `azure/login` step using `AZURE_WEBSITE_CLIENT_ID`, `AZURE_WEBSITE_TENANT_ID`, `AZURE_WEBSITE_SUBSCRPITION_ID`.
- [x] 2.4 Add a deploy step that publishes the Hugo output directory to the Azure Static Web Apps resource's production environment using the OIDC-authenticated session (no deployment-token secret).
- [x] 2.5 Add a code comment near the secret references noting that `AZURE_WEBSITE_SUBSCRPITION_ID`'s spelling matches the already-provisioned secret and must not be "corrected".

## 3. Documentation

- [x] 3.1 Update `README.md`'s `.github/workflows/` row to say `website.yml` builds and deploys the Hugo site to Azure Static Web Apps on push to `main` under `src/website/**` (remove the current `docs/` mention, which doesn't match this workflow's trigger).
- [x] 3.2 Add a short "Deployment" note to `src/website/README.md` (or create one if it doesn't exist) pointing at `.github/workflows/website.yml` and this change's prerequisites for anyone who needs to reprovision Azure resources.

## 4. Verification

- [ ] 4.1 Push a trivial change under `src/website/` to a branch, open a PR to confirm the workflow does NOT run on the PR (production-only trigger), then merge to `main` and confirm the workflow runs.
- [ ] 4.2 Confirm the deployed site is reachable at the Azure Static Web Apps default hostname and reflects the latest `main` content.
- [ ] 4.3 Confirm an unrelated Android-only commit to `main` does not trigger `website.yml`, and that `ci.yml`/`release.yml` are unaffected by the new workflow.
