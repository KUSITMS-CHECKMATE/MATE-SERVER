variable "resource_group_name" {
  description = "Resource Group for Key Vault."
  type        = string
}

variable "location" {
  description = "Azure region."
  type        = string
}

variable "key_vault_name" {
  description = "Globally unique vault name"
  type        = string
}

variable "sku_name" {
  description = "Key Vault SKU"
  type        = string
  default     = "standard"

  validation {
    condition     = contains(["standard", "premium"], lower(var.sku_name))
    error_message = "sku_name must be standard or premium."
  }
}

variable "soft_delete_retention_days" {
  description = "Soft-delete retention in days."
  type        = number
  default     = 90
}

variable "purge_protection_enabled" {
  description = "true: a soft-deleted vault cannot be permanently purged (stricter)."
  type        = bool
  default     = false
}

variable "public_network_access_enabled" {
  description = "Allow public network access (set false when using private endpoints)."
  type        = bool
  default     = true
}
