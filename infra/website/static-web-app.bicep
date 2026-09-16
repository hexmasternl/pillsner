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
    // integration. Content is pushed by .github/workflows/website.yml, which fetches a
    // deployment token separately via `az staticwebapp secrets list` after this resource
    // exists - see main.bicep's comment on why that's not done as a Bicep output here.
    stagingEnvironmentPolicy: 'Enabled'
  }
}

output staticWebAppName string = staticSite.name
output defaultHostname string = staticSite.properties.defaultHostname
