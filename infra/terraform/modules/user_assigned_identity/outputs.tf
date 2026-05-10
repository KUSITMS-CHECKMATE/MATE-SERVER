output "id" {
  description = "Managed identity resource id."
  value       = azurerm_user_assigned_identity.this.id
}

output "name" {
  description = "Managed identity name."
  value       = azurerm_user_assigned_identity.this.name
}

output "principal_id" {
  description = "Object (principal) id — use with azurerm_role_assignment."
  value       = azurerm_user_assigned_identity.this.principal_id
}

output "client_id" {
  description = "Client id — use for DefaultAzureCredential / workload apps."
  value       = azurerm_user_assigned_identity.this.client_id
}

output "tenant_id" {
  description = "Entra tenant id."
  value       = azurerm_user_assigned_identity.this.tenant_id
}
