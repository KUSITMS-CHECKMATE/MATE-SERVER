output "vnet_id" {
  description = "Created Virtual Network id."
  value       = azurerm_virtual_network.this.id
}

output "vnet_name" {
  description = "Created Virtual Network name."
  value       = azurerm_virtual_network.this.name
}

output "app_subnet_id" {
  description = "Application subnet id."
  value       = azurerm_subnet.app.id
}

output "app_subnet_name" {
  description = "Application subnet name."
  value       = azurerm_subnet.app.name
}

output "db_subnet_id" {
  description = "Database subnet id."
  value       = azurerm_subnet.db.id
}

output "db_subnet_name" {
  description = "Database subnet name."
  value       = azurerm_subnet.db.name
}
