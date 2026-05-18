variable "resource_group_name" {
  description = "Resource group for Azure Cache for Redis."
  type        = string
}

variable "location" {
  description = "Azure region."
  type        = string
}

variable "redis_cache_name" {
  description = "Globally unique Azure Cache for Redis name."
  type        = string
}

variable "virtual_network_id" {
  description = "Virtual network id to link with the Redis private DNS zone."
  type        = string
}

variable "private_endpoint_subnet_id" {
  description = "Subnet id where the Redis private endpoint will be created."
  type        = string
}

variable "private_dns_zone_name" {
  description = "Private DNS zone name for Azure Cache for Redis private endpoint."
  type        = string
  default     = "privatelink.redis.cache.windows.net"
}

variable "private_dns_zone_link_name" {
  description = "VNet link name for Redis private DNS zone."
  type        = string
  default     = "redis-private-dns-zone-link"
}

variable "capacity" {
  description = "Redis cache capacity. 0 is the smallest size for Basic/Standard."
  type        = number
  default     = 0
}

variable "family" {
  description = "Redis cache family. C is Basic/Standard."
  type        = string
  default     = "C"
}

variable "sku_name" {
  description = "Redis cache SKU."
  type        = string
  default     = "Basic"
}

variable "minimum_tls_version" {
  description = "Minimum TLS version for Redis connections."
  type        = string
  default     = "1.2"
}

variable "tags" {
  description = "Optional resource tags."
  type        = map(string)
  default     = {}
}
