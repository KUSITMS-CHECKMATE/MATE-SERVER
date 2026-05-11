output "id" {
  description = "ACR resource id."
  value       = azurerm_container_registry.this.id
}

output "name" {
  description = "ACR resource name."
  value       = azurerm_container_registry.this.name
}

output "login_server" {
  description = "Login server hostname (e.g. myregistry.azurecr.io)."
  value       = azurerm_container_registry.this.login_server
}
