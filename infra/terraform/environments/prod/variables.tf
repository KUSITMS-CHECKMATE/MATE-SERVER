variable "resource_group_name" {
  description = "Existing Resource Group name to reference only."
  type        = string
}

variable "location" {
  description = "Azure region used for deployable resources."
  type        = string
}

variable "vnet_name" {
  description = "Virtual Network name."
  type        = string
}

variable "vnet_cidr" {
  description = "Virtual Network CIDR block."
  type        = string
}

variable "app_subnet_name" {
  description = "Application subnet name for AKS nodes."
  type        = string
}

variable "app_subnet_cidr" {
  description = "Application subnet CIDR block."
  type        = string
}

variable "db_subnet_name" {
  description = "Database subnet name for PostgreSQL Flexible Server."
  type        = string
}

variable "db_subnet_cidr" {
  description = "Database subnet CIDR block."
  type        = string
}

variable "postgres_private_dns_zone_name" {
  description = "Private DNS zone name for PostgreSQL private access."
  type        = string
}

variable "postgres_private_dns_zone_link_name" {
  description = "VNet link name for PostgreSQL private DNS zone."
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
  description = "PostgreSQL admin password. Provide via TF_VAR_postgres_admin_password."
  type        = string
  sensitive   = true
}

variable "postgres_version" {
  description = "PostgreSQL engine version."
  type        = string
}

variable "postgres_sku_name" {
  description = "PostgreSQL Flexible Server SKU."
  type        = string
}

variable "postgres_storage_mb" {
  description = "PostgreSQL storage size in MB."
  type        = number
}

variable "postgres_backup_retention_days" {
  description = "PostgreSQL backup retention in days."
  type        = number
}

variable "storage_account_name" {
  description = "Existing Storage Account name to reference."
  type        = string
}

variable "storage_container_name" {
  description = "Blob container name used by the application."
  type        = string
}

variable "acr_name" {
  description = "Globally unique ACR name"
  type        = string
}

variable "acr_sku" {
  description = "Azure Container Registry SKU."
  type        = string
  default     = "Basic"
}

variable "acr_admin_enabled" {
  description = "If true, enables admin credentials (prefer false when using AcrPull)."
  type        = bool
  default     = false
}

variable "key_vault_name" {
  description = "Globally unique Key Vault name"
  type        = string
}

variable "app_managed_identity_name" {
  description = "User-assigned managed identity name (RG scope)."
  type        = string
}
