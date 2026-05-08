variable "resource_group_name" {
  description = "Existing Resource Group name to reference only."
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
