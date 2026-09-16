targetScope = 'subscription'

@description('Azure region for the resource group and the Static Web App.')
param location string = 'westeurope'

@description('Name of the resource group that holds the Pillsner website resources.')
param resourceGroupName string = 'rg-pillsner-website'

@description('Name of the Azure Static Web Apps resource.')
param staticWebAppName string = 'pillsner-website'

resource rg 'Microsoft.Resources/resourceGroups@2025-04-01' = {
  name: resourceGroupName
  location: location
}

module staticWebApp 'static-web-app.bicep' = {
  name: 'static-web-app'
  scope: rg
  params: {
    location: location
    staticWebAppName: staticWebAppName
  }
}

// The deployment token deliberately isn't a template output: an earlier version routed
// staticSite.listSecrets() through a @secure() output on this subscription-scoped template via
// a nested module, and swa deploy rejected the value it got back with "deployment_token
// provided was invalid" - ARM's handling of secure outputs across a module boundary at this
// scope didn't round-trip the real secret. The workflow instead calls
// `az staticwebapp secrets list` directly against the resource these outputs identify, which is
// the pattern Microsoft's own CI/CD docs recommend for this exact purpose.
output resourceGroupName string = rg.name
output staticWebAppName string = staticWebApp.outputs.staticWebAppName
output defaultHostname string = staticWebApp.outputs.defaultHostname
