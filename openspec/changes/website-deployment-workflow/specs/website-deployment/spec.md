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
The workflow SHALL authenticate to Azure using OpenID Connect federated credentials, sourcing the subscription ID, tenant ID and client ID from the repository secrets `AZURE_WEBSITE_SUBSCRIPTION_ID`, `AZURE_WEBSITE_TENANT_ID` and `AZURE_WEBSITE_CLIENT_ID` respectively. The workflow SHALL NOT require or use a client secret, certificate, or long-lived deployment-token secret stored in GitHub for this authentication.

#### Scenario: Successful OIDC authentication
- **WHEN** the workflow runs on `main` with a valid federated credential trusting this repository already configured in Azure
- **THEN** the workflow obtains an Azure access token without any client secret or deployment token being present in the repository's secrets

#### Scenario: Missing or misconfigured federated credential
- **WHEN** the federated credential trusting this repository does not exist or does not match the workflow's OIDC token subject
- **THEN** the Azure login step fails with an authentication error and the deployment step does not run

### Requirement: Deployment target is Azure Static Web Apps Free tier
The workflow SHALL deploy the built site to an Azure Static Web Apps resource on the Free tier's production environment. The workflow SHALL NOT create, upgrade, or otherwise modify any plan tier other than Free.

#### Scenario: Deploy after provisioning
- **WHEN** the workflow's infrastructure step has provisioned (or confirmed) the Azure Static Web Apps Free tier resource
- **THEN** the built site content is published to that resource's production environment and becomes reachable at its URL

### Requirement: Resource provisioning is automated and idempotent
The workflow SHALL provision the resource group and the Azure Static Web Apps resource it deploys to using a declarative Infrastructure-as-Code template, run as part of the workflow itself. Running the provisioning step against infrastructure that already matches the template SHALL be a no-op with respect to that infrastructure's configuration, and SHALL NOT fail because the resources already exist.

#### Scenario: First run creates the infrastructure
- **WHEN** the workflow runs and the resource group and Static Web App do not yet exist
- **THEN** the provisioning step creates both, and the workflow proceeds to build and deploy without manual intervention

#### Scenario: Later runs reconcile without side effects
- **WHEN** the workflow runs again and the resource group and Static Web App already match the template
- **THEN** the provisioning step succeeds without creating duplicate resources or altering unrelated configuration

#### Scenario: Provisioning failure stops the deployment
- **WHEN** the provisioning step fails (for example, the workflow's identity lacks permission to create resources)
- **THEN** the workflow fails at that step and no build output is deployed

### Requirement: Deployment credential is never stored as a long-lived GitHub secret
The workflow SHALL obtain the Azure Static Web Apps deployment token dynamically, by querying the provisioned resource directly, on every run. The workflow SHALL NOT read a deployment token from a GitHub Actions secret, and the token SHALL NOT be written to deployment history, workflow logs, or any uploaded artifact.

#### Scenario: Token fetched after provisioning
- **WHEN** the provisioning step has completed successfully
- **THEN** the workflow fetches a fresh deployment token for that resource and passes it directly to the deploy step within the same job run, without persisting it anywhere outside that run

#### Scenario: Token does not appear in logs
- **WHEN** the workflow run's logs are inspected after a successful or failed run
- **THEN** the deployment token's value does not appear in plain text anywhere in those logs
