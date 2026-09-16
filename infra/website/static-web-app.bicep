@description('Azure region for the Static Web App.')
param location string

@description('Name of the Azure Static Web Apps resource.')
param staticWebAppName string

resource staticSite 'Microsoft.Web/staticSites@2024-11-01' = {
  name: staticWebAppName
  location: location
  sku: {
    name: 'Free'
    tier: 'Free'
  }
  properties: {
    // No repositoryUrl/branch: this resource isn't wired to GitHub's own build-and-deploy
    // integration. Content is pushed by .github/workflows/website.yml via the deployment
    // token below instead.
    stagingEnvironmentPolicy: 'Enabled'
  }
}

output staticWebAppName string = staticSite.name
output defaultHostname string = staticSite.properties.defaultHostname

@secure()
output deploymentToken string = staticSite.listSecrets().properties.apiKey
