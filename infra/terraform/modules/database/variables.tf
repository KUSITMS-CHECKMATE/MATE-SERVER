variable "resource_group_name" {
  description = "Resource Group name for database networking resources."
  type        = string
}

variable "location" {
  description = "Azure region for database resources."
  type        = string
}

variable "virtual_network_id" {
  description = "Virtual Network id linked to the PostgreSQL private DNS zone."
  type        = string
}

variable "db_subnet_id" {
  description = "Delegated subnet id for PostgreSQL Flexible Server."
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

variable "postgres_server_name" {
  description = "PostgreSQL Flexible Server name."
  type        = string
}

variable "postgres_database_name" {
  description = "Application database name."
  type        = string
}

variable "postgres_admin_username" {
  description = "PostgreSQL admin username."
  type        = string
}

variable "postgres_admin_password" {
  description = "PostgreSQL admin password."
  type        = string
  sensitive   = true
}

variable "postgres_version" {
  description = "PostgreSQL engine version."
  type        = string
  default     = "16"
}

variable "postgres_sku_name" {
  description = "PostgreSQL Flexible Server SKU."
  type        = string
  default     = "B_Standard_B1ms"
}

variable "postgres_storage_mb" {
  description = "PostgreSQL storage size in MB."
  type        = number
  default     = 32768
}

variable "postgres_backup_retention_days" {
  description = "PostgreSQL backup retention in days."
  type        = number
  default     = 7
}
