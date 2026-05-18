output "id" {
  description = "Azure Cache for Redis resource id."
  value       = azurerm_redis_cache.this.id
}

output "name" {
  description = "Azure Cache for Redis name."
  value       = azurerm_redis_cache.this.name
}

output "hostname" {
  description = "Redis hostname."
  value       = azurerm_redis_cache.this.hostname
}

output "ssl_port" {
  description = "Redis TLS port."
  value       = azurerm_redis_cache.this.ssl_port
}

output "primary_access_key" {
  description = "Redis primary access key."
  value       = azurerm_redis_cache.this.primary_access_key
  sensitive   = true
}

output "private_dns_zone_id" {
  description = "Redis private DNS zone id."
  value       = azurerm_private_dns_zone.redis.id
}

output "private_endpoint_id" {
  description = "Redis private endpoint id."
  value       = azurerm_private_endpoint.redis.id
}

output "private_endpoint_ip_address" {
  description = "Redis private endpoint IP address."
  value       = azurerm_private_endpoint.redis.private_service_connection[0].private_ip_address
}
