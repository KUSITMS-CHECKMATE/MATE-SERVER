variable "resource_group_name" {
  description = "Resource Group name where networking resources will be created."
  type        = string
}

variable "location" {
  description = "Azure region for networking resources."
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
