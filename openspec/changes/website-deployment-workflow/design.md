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
The workflow requests a GitHub OIDC token and exchanges it for an Azure access token via `azure/login@v2` with `client-id`, `tenant-id`, and `subscription-id` sourced from `AZURE_WEBSITE_CLIENT_ID`, `AZURE_WEBSITE_TENANT_ID`, and `AZURE_WEBSITE_SUBSCRIPTION_ID`. This requires `permissions: id-token: write` on the job. No `client-secret` input is set, since none of the three provided secrets is a client secret — the design assumes (and Decision 5 states) that a federated identity credential already trusts this repository and the `main` branch.

**Alternative considered:** the classic `Azure/static-web-apps-deploy@v1` action with a single `AZURE_STATIC_WEB_APPS_API_TOKEN` deployment-token secret. Rejected: that's a different, static-secret-based credential than what's already provisioned (subscription/tenant/client id), and OIDC is Microsoft's current recommended pattern (short-lived tokens, no secret to leak or rotate).

### 3. Provision via a subscription-scoped Bicep template, then deploy with the token it outputs
`infra/website/main.bicep` (`targetScope = 'subscription'`) creates the resource group and delegates to `infra/website/static-web-app.bicep` (resource-group scope) to create the Free-tier `Microsoft.Web/staticSites` resource with no `repositoryUrl` (this workflow pushes content itself; the resource isn't wired to GitHub's own build-and-deploy integration). The module outputs the resource's deployment token as a `@secure()` output — `staticSite.listSecrets().properties.apiKey` — which keeps it out of deployment history and CLI logging while still being returned to the immediate `az deployment sub create --query properties.outputs -o json` call that provisioned it. The workflow captures that value once, masks it (`::add-mask::`), and passes it straight to `swa deploy --deployment-token`.

Both the resource group and the static site resources are declared directly (`= { ... }`, not `existing`), so `az deployment sub create` is idempotent: a first run creates everything, every later run is a no-op reconciliation that still returns a fresh deployment token.

**Alternative considered (from an earlier version of this design):** authenticate with `azure/login`, then call `swa deploy` directly with `--subscription-id`/`--tenant-id`/`--client-id`, relying on the CLI's own AAD login. Rejected after an actual run: `swa deploy`'s AAD path is interactive-first — when it can't resolve the named app (because it doesn't exist yet), it drops into a resource picker prompt with no way to answer it on a non-interactive runner, so the job just hangs instead of failing. A deployment token sidesteps that path entirely.

**Alternative considered:** `Azure/static-web-apps-deploy@v1` action with a manually-created, portal-issued deployment token stored as a GitHub secret. Rejected: that reintroduces a long-lived secret the OIDC approach was chosen to avoid, and still requires the resource to be created out-of-band first.

### 4. Hugo version pin
The workflow installs the same Hugo version already pinned in `README.md`'s toolchain table (`0.165.0`, extended edition) via `peaceiris/actions-hugo` or manual download, so CI output matches local `hugo --minify` output. If that version is bumped, both `README.md` and this workflow must move together — same rule the project already applies to the Android version catalog.

### 5. Identity/RBAC provisioning stays manual; resource provisioning does not
Two different things were conflated in an earlier version of this design. Provisioning the *identity* (the Entra app registration, its federated credential trusting this repo/branch, and the RBAC role assignment letting it create resources) is a one-time, low-frequency operation with real security consequences and has to happen outside any workflow this app registration itself runs — a credential can't grant itself access. That stays manual, documented as a prerequisite.

Provisioning the *resources* (the resource group, the Static Web App) is exactly the kind of repeatable, declarative, idempotent operation Infrastructure-as-Code exists for, and doing it by hand first is what caused the interactive-prompt failure in Decision 3. So this design has the workflow provision the resources itself via Bicep on every run, while the identity/RBAC setup remains a manual, one-time step. Creating a resource group at subscription scope requires the app registration to hold a role with `Microsoft.Resources/subscriptions/resourceGroups/write` at the **subscription** scope (in practice, `Contributor` on the subscription, since Static Web Apps free tier has no narrower built-in role for this) — broader than "deploy content to one resource," and worth noting as a real trade-off (see Risks).

## Risks / Trade-offs

- **[Risk]** The federated credential's trust condition must match exactly how GitHub issues the OIDC token for this workflow (repo + `main` branch, or an `environment:` subject if a GitHub Environment is used). A mismatch fails `azure/login` with an opaque AADSTS error. → **Mitigation:** document the exact subject format in the prerequisites, and keep the workflow's trigger (`main` branch push) consistent with whatever subject the credential trusts.
- **[Risk]** Letting the workflow's own identity create resource groups requires subscription-scope `Contributor` (or equivalent), which is broader than "deploy content to one Static Web App" — a compromised or over-broadly-reused credential could affect other resources in the subscription. → **Mitigation:** the app registration should be dedicated to this repository/workflow only (not shared with other automation), and the federated credential's trust condition should be scoped tightly to this repo and `main`.
- **[Risk]** A `@secure()` Bicep output keeps the deployment token out of deployment history and normal logging, but it is still plain text in the runner's process memory/environment for the run's duration, and `::add-mask::` only prevents GitHub from *printing* it in logs — it doesn't prevent a step that mishandles it from leaking it. → **Mitigation:** the token is used immediately and only within this job, never written to a file that's uploaded as an artifact or echoed outside the masked step.
- **[Risk]** Free tier Static Web Apps has a bandwidth/storage cap; if the site later grows (more locales, larger media), it could hit those limits. → **Mitigation:** out of scope for now (the site is a handful of static pages); revisit the plan tier in a future change if usage data warrants it.
- **[Risk]** Without a PR preview or staging step, a broken Hugo build or bad content only surfaces after merging to `main`. → **Mitigation:** `ci.yml`-independent but still CI-checked: a future change could add a build-only (no deploy) job on pull requests touching `src/website/**`; noted as a possible follow-up, not required here since the request scoped this to `main`-only deployment.

## Migration Plan

- No data migration. This is a net-new CI/CD capability.
- Rollout: merge the workflow and the Bicep templates; a maintainer completes the one-time identity/RBAC prerequisite (app registration, federated credential, subscription-scope role assignment) before or immediately after merge. Until that identity exists, `azure/login` fails visibly — an expected, visible failure rather than a silent no-op. Once it exists, the first run provisions the resource group and Static Web App itself.
- Rollback: delete `.github/workflows/website.yml` and `infra/website/` (and, if desired, the Azure resources and the federated credential/app registration) with no impact on the Android app or any other workflow.

## Open Questions

- Should the Static Web Apps resource name/resource group be stored as GitHub Actions **variables** (`vars.*`) instead of hardcoded in the workflow, for easier renaming later? Leaning yes; left for `tasks.md` since it's a low-risk implementation detail.
- Should a PR-time "build only, don't deploy" check be added later so a broken site is caught before merge? Left as a noted follow-up (see Risks), not part of this change's scope.
- Is subscription-scope `Contributor` for the app registration acceptable long-term, or should a narrower custom role (just `resourceGroups/write` + `Microsoft.Web/staticSites/*`) replace it once the one-time setup is stable? Left open; not a blocker for this change.
