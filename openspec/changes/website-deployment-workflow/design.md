## Context

`website-single-page-hugo` (not yet archived) adds a Hugo site at `src/website/` and a local build (`hugo --minify`), but explicitly defers hosting/deployment: "Deployment (choosing and configuring a static host) is out of scope for this change." `README.md` already documents `.github/workflows/website.yml` as building the site when `src/website/` or `docs/` changes — that text was written ahead of the workflow existing and currently overstates both scope (it mentions `docs/`) and effect (it says "builds", not "deploys"). This change creates the actual workflow, corrects that README text, and picks the static host the earlier design left open: Azure Static Web Apps.

The repository already has `AZURE_WEBSITE_SUBSCRIPTION_ID`, `AZURE_WEBSITE_TENANT_ID` and `AZURE_WEBSITE_CLIENT_ID` configured as GitHub secrets. Three secrets with no accompanying client secret is the standard shape for OpenID Connect (workload identity federation): a Microsoft Entra app registration with a federated credential trusting GitHub's OIDC token for this repository, and Azure RBAC granting that app registration access. No password, certificate, or long-lived deployment token needs to live in GitHub.

A first implementation attempt authenticated via `azure/login` and then called `swa deploy` directly against a not-yet-existing Static Web App by name. `swa deploy`'s AAD login path is built for interactive/local use: when it can't resolve the named app, it falls back to an interactive resource picker, which just hangs a non-interactive GitHub Actions runner (confirmed by an actual failed run). That, plus the resource never having been provisioned, is why this design now provisions the infrastructure itself via Bicep rather than treating it as a manual prerequisite.

## Goals / Non-Goals

**Goals:**
- A GitHub Actions workflow that builds `src/website/` with the pinned Hugo version and publishes the output to an Azure Static Web Apps (Free tier) resource on every push to `main` that touches `src/website/**`.
- Authenticate to Azure using only the three existing OIDC secrets — no new secret type introduced.
- Provision the resource group and Static Web App via a subscription-scoped Bicep template, idempotently, as part of the workflow — no manual "click the portal first" step.
- Keep the workflow fully independent of `ci.yml` and `release.yml`: separate file, separate triggers, no shared jobs, caches, or working directories.
- Support a manual re-run (`workflow_dispatch`) for redeploying without a content change (e.g. after rotating the federated credential).
- Leave the workflow able to fail loudly and stop if authentication or provisioning fails, rather than silently no-op-ing or hanging.

**Non-Goals:**
- Provisioning the Entra app registration/federated credential itself, or the RBAC role assignment that lets it run the Bicep deployment. Those identity-level prerequisites remain one-time manual setup (see Decision 5); only the Azure *resources* (resource group, Static Web App) are provisioned by this workflow.
- Preview/staging environments for pull requests. This change only wires up production deployment on `main`; PR preview environments (a Static Web Apps feature) can be a follow-up.
- Broadening the trigger to `docs/**` or any other path. The proposal scopes the trigger to `src/website/**` only, per the request; the earlier README wording that mentioned `docs/` was aspirational and is corrected, not implemented.
- Custom domain configuration, CDN, or WAF setup in front of the Static Web Apps resource.
- Changing anything under `src/app`, `src/wear`, `src/shared`, or the Android release pipeline.

## Decisions

### 1. Trigger: `push` to `main`, path-filtered to `src/website/**`, plus `workflow_dispatch`
Matches the request exactly (deploy on `main` changes under `src/website`) and avoids deploying on every unrelated Android commit. `workflow_dispatch` is added so a maintainer can force a redeploy (e.g. after a secret rotation) without an empty commit. Pull requests are intentionally not a trigger for this change — production-only deployment keeps the scope minimal; PR previews are a Non-Goal above.

**Alternative considered:** trigger on `docs/**` too, matching the README's current text. Rejected for this change because it wasn't part of the request and would deploy the live site on unrelated design-system doc edits; the README text is corrected instead of implemented.

### 2. Authentication: `azure/login` with OIDC, using the three existing secrets
The workflow requests a GitHub OIDC token and exchanges it for an Azure access token via `azure/login@v2` with `client-id`, `tenant-id`, and `subscription-id` sourced from `AZURE_WEBSITE_CLIENT_ID`, `AZURE_WEBSITE_TENANT_ID`, and `AZURE_WEBSITE_SUBSCRPITION_ID`. This requires `permissions: id-token: write` on the job. No `client-secret` input is set, since none of the three provided secrets is a client secret — the design assumes (and the prerequisites section states) that a federated identity credential already trusts this repository and the `main` branch.

**Alternative considered:** the classic `Azure/static-web-apps-deploy@v1` action with a single `AZURE_STATIC_WEB_APPS_API_TOKEN` deployment-token secret. Rejected: that's a different, static-secret-based credential than what's already provisioned (subscription/tenant/client id), and OIDC is Microsoft's current recommended pattern (short-lived tokens, no secret to leak or rotate).

### 3. Deploy step: Azure Static Web Apps CLI (`swa deploy`) authenticated via the OIDC session
After `azure/login`, the workflow runs `hugo --minify` from `src/website/` to produce the output directory, then deploys that directory with `npx --yes @azure/static-web-apps-cli deploy <output-dir> --app-name <swa-name> --resource-group <rg> --subscription-id ... --tenant-id ... --client-id ... --env production`, relying on the Azure CLI session `azure/login` already established rather than a deployment token. The Static Web Apps resource name and resource group are supplied as workflow-level values (not secrets, since they aren't sensitive) — the exact mechanism (repository variables vs. hardcoded in the workflow) is left to `tasks.md`/implementation, since it doesn't change the design.

**Alternative considered:** `Azure/static-web-apps-deploy@v1` action. It's the more common turnkey path but is built around the deployment-token flow; using it with OIDC-only credentials is not its documented use case. `swa deploy` explicitly documents `--subscription-id`/`--tenant-id`/`--client-id` flags for Azure AD-based deployment, making it the better fit for the credentials already available.

### 4. Hugo version pin
The workflow installs the same Hugo version already pinned in `README.md`'s toolchain table (`0.165.0`, extended edition) via `peaceiris/actions-hugo` or manual download, so CI output matches local `hugo --minify` output. If that version is bumped, both `README.md` and this workflow must move together — same rule the project already applies to the Android version catalog.

### 5. Resource provisioning stays manual, documented as a prerequisite
Provisioning Azure infrastructure (the Static Web Apps resource, the app registration, the federated credential, the RBAC role assignment) is a one-time, low-frequency operation with real security consequences (who can deploy, what the app registration can touch). Automating it via Bicep/Terraform in this change would expand scope well beyond "add a deployment workflow" and isn't something CI should be able to silently re-provision. `tasks.md` includes the manual steps as an explicit prerequisite checklist, verified before the workflow is expected to succeed, rather than as workflow code.

## Risks / Trade-offs

- **[Risk]** The federated credential's trust condition must match exactly how GitHub issues the OIDC token for this workflow (repo + `main` branch, or an `environment:` subject if a GitHub Environment is used). A mismatch fails `azure/login` with an opaque AADSTS error. → **Mitigation:** document the exact subject format in the prerequisites, and keep the workflow's trigger (`main` branch push) consistent with whatever subject the credential trusts.
- **[Risk]** `AZURE_WEBSITE_SUBSCRPITION_ID` has a typo baked into its name; a future contributor might "fix" it in one place and break the workflow. → **Mitigation:** reference the exact secret name in a code comment in the workflow file and in this design, so the typo is clearly intentional (matches what's already provisioned) rather than a bug to silently correct.
- **[Risk]** Free tier Static Web Apps has a bandwidth/storage cap; if the site later grows (more locales, larger media), it could hit those limits. → **Mitigation:** out of scope for now (the site is a handful of static pages); revisit the plan tier in a future change if usage data warrants it.
- **[Risk]** Without a PR preview or staging step, a broken Hugo build or bad content only surfaces after merging to `main`. → **Mitigation:** `ci.yml`-independent but still CI-checked: a future change could add a build-only (no deploy) job on pull requests touching `src/website/**`; noted as a possible follow-up, not required here since the request scoped this to `main`-only deployment.

## Migration Plan

- No data migration. This is a net-new CI/CD capability.
- Rollout: merge the workflow; a maintainer completes the one-time Azure prerequisites (resource + federated credential) before or immediately after merge. Until the prerequisites exist, the workflow will run and fail at the `azure/login` or deploy step — an expected, visible failure rather than a silent no-op.
- Rollback: delete `.github/workflows/website.yml` (and, if desired, the Azure resource and federated credential) with no impact on the Android app or any other workflow.

## Open Questions

- Should the Static Web Apps resource name/resource group be stored as GitHub Actions **variables** (`vars.*`) instead of hardcoded in the workflow, for easier renaming later? Leaning yes; left for `tasks.md` since it's a low-risk implementation detail.
- Should a PR-time "build only, don't deploy" check be added later so a broken site is caught before merge? Left as a noted follow-up (see Risks), not part of this change's scope.
