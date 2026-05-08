output "resource_group_id" {
  description = "Referenced existing Resource Group id."
  value       = module.resource_group.id
}

output "resource_group_name" {
  description = "Referenced existing Resource Group name."
  value       = module.resource_group.name
}

output "resource_group_location" {
  description = "Referenced existing Resource Group location."
  value       = module.resource_group.location
}

output "vnet_id" {
  description = "Created Virtual Network id."
  value       = module.networking.vnet_id
}

output "app_subnet_id" {
  description = "Application subnet id for AKS."
  value       = module.networking.app_subnet_id
}

output "db_subnet_id" {
  description = "Database subnet id for PostgreSQL."
  value       = module.networking.db_subnet_id
}
