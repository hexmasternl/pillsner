## ADDED Requirements

### Requirement: Deployment triggers on website changes to main
The system SHALL run a dedicated GitHub Actions workflow that builds and deploys `src/website/` whenever a push to the `main` branch changes one or more files under `src/website/**`. The workflow SHALL also be runnable on demand via `workflow_dispatch` without requiring a matching file change.

#### Scenario: Push to main touches the website
- **WHEN** a commit is pushed to `main` that modifies a file under `src/website/`
- **THEN** the website deployment workflow runs and deploys the resulting build

#### Scenario: Push to main does not touch the website
- **WHEN** a commit is pushed to `main` that modifies only files outside `src/website/` (for example, Android app code)
- **THEN** the website deployment workflow does not run

#### Scenario: Manual redeploy
- **WHEN** a maintainer triggers the workflow manually via `workflow_dispatch`
- **THEN** the workflow builds and deploys the current `main` content of `src/website/` regardless of whether it changed

### Requirement: Isolation from other workflows
The website deployment workflow SHALL be defined in its own workflow file, separate from `ci.yml` and `release.yml`, and SHALL NOT share jobs, working directories, or cache keys with the Android CI/CD workflows.

#### Scenario: Android-only change
- **WHEN** a commit changes only Android app files and does not touch `src/website/`
- **THEN** `ci.yml` and/or `release.yml` run as before and the website deployment workflow does not run

#### Scenario: Website-only change
- **WHEN** a commit changes only files under `src/website/`
- **THEN** the website deployment workflow runs and the Android CI/CD workflows are unaffected by it

### Requirement: Hugo build produces the deployable output
The workflow SHALL build the site from `src/website/` using the same Hugo version documented in the project's toolchain reference, producing a minified static output directory before any deployment step runs.

#### Scenario: Build precedes deploy
- **WHEN** the workflow runs
- **THEN** the Hugo build step completes successfully before the deployment step starts, and the deployment step publishes that build's output directory

#### Scenario: Build failure stops deployment
- **WHEN** the Hugo build step fails (for example, a template or content error)
- **THEN** the workflow fails and no deployment step runs

### Requirement: Azure authentication via OpenID Connect
The workflow SHALL authenticate to Azure using OpenID Connect federated credentials, sourcing the subscription ID, tenant ID and client ID from the repository secrets `AZURE_WEBSITE_SUBSCRPITION_ID`, `AZURE_WEBSITE_TENANT_ID` and `AZURE_WEBSITE_CLIENT_ID` respectively. The workflow SHALL NOT require or use a client secret, certificate, or long-lived deployment-token secret for this authentication.

#### Scenario: Successful OIDC authentication
- **WHEN** the workflow runs on `main` with a valid federated credential trusting this repository already configured in Azure
- **THEN** the workflow obtains an Azure access token without any client secret or deployment token being present in the repository's secrets

#### Scenario: Missing or misconfigured federated credential
- **WHEN** the federated credential trusting this repository does not exist or does not match the workflow's OIDC token subject
- **THEN** the Azure login step fails with an authentication error and the deployment step does not run

### Requirement: Deployment target is Azure Static Web Apps Free tier
The workflow SHALL deploy the built site to an Azure Static Web Apps resource provisioned on the Free tier's production environment. The workflow SHALL NOT provision, upgrade, or otherwise modify the Azure Static Web Apps resource's plan tier.

#### Scenario: Deploy to existing Free tier resource
- **WHEN** the workflow's deploy step runs against an already-provisioned Azure Static Web Apps Free tier resource
- **THEN** the built site content is published to that resource's production environment and becomes reachable at its URL

#### Scenario: Target resource does not exist
- **WHEN** the configured Azure Static Web Apps resource has not been provisioned yet
- **THEN** the deploy step fails visibly rather than silently succeeding or creating a new resource
