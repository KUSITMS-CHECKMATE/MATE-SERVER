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

output "postgres_private_dns_zone_id" {
  description = "PostgreSQL private DNS zone id."
  value       = module.database.postgres_private_dns_zone_id
}

output "postgres_private_dns_zone_link_id" {
  description = "PostgreSQL private DNS zone VNet link id."
  value       = module.database.postgres_private_dns_zone_link_id
}

output "postgres_server_id" {
  description = "PostgreSQL Flexible Server id."
  value       = module.database.postgres_server_id
}

output "postgres_server_fqdn" {
  description = "PostgreSQL Flexible Server FQDN."
  value       = module.database.postgres_server_fqdn
}

output "postgres_database_id" {
  description = "PostgreSQL application database id."
  value       = module.database.postgres_database_id
}

output "storage_account_name" {
  description = "Referenced Storage Account name."
  value       = module.storage.storage_account_name
}

output "storage_blob_endpoint" {
  description = "Blob endpoint for the referenced Storage Account."
  value       = module.storage.primary_blob_endpoint
}

output "storage_container_name" {
  description = "Blob container name used by the application."
  value       = module.storage.storage_container_name
}

output "acr_id" {
  description = "Azure Container Registry resource id."
  value       = module.container_registry.id
}

output "acr_name" {
  description = "Azure Container Registry name."
  value       = module.container_registry.name
}

output "acr_login_server" {
  description = "ACR login server (image host)."
  value       = module.container_registry.login_server
}
