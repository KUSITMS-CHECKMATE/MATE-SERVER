variable "resource_group_name" {
  description = "Resource Group name for database networking resources."
  type        = string
}

variable "virtual_network_id" {
  description = "Virtual Network id linked to the PostgreSQL private DNS zone."
  type        = string
}

variable "postgres_private_dns_zone_name" {
  description = "Private DNS zone name for PostgreSQL Flexible Server private access."
  type        = string
  default     = "privatelink.postgres.database.azure.com"
}

variable "postgres_private_dns_zone_link_name" {
  description = "Name of the VNet link for PostgreSQL private DNS zone."
  type        = string
}
