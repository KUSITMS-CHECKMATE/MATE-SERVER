output "name" {
  description = "Referenced Resource Group name."
  value       = data.azurerm_resource_group.this.name
}

output "location" {
  description = "Referenced Resource Group location."
  value       = data.azurerm_resource_group.this.location
}

output "id" {
  description = "Referenced Resource Group id."
  value       = data.azurerm_resource_group.this.id
}
