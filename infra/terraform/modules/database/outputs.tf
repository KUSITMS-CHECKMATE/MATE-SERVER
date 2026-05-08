output "postgres_private_dns_zone_id" {
  description = "PostgreSQL private DNS zone id."
  value       = azurerm_private_dns_zone.postgres.id
}

output "postgres_private_dns_zone_name" {
  description = "PostgreSQL private DNS zone name."
  value       = azurerm_private_dns_zone.postgres.name
}

output "postgres_private_dns_zone_link_id" {
  description = "VNet link id for PostgreSQL private DNS zone."
  value       = azurerm_private_dns_zone_virtual_network_link.postgres.id
}
