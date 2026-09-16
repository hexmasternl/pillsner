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

output resourceGroupName string = rg.name
output staticWebAppName string = staticWebApp.outputs.staticWebAppName
output defaultHostname string = staticWebApp.outputs.defaultHostname

@secure()
output deploymentToken string = staticWebApp.outputs.deploymentToken
