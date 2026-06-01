variable "resource_group_name" {
  description = "Resource group where the Storage Account is created."
  type        = string
}

variable "location" {
  description = "Azure region for the Storage Account (must align with RG policy / data residency)."
  type        = string
}

variable "storage_account_name" {
  description = "Storage Account name (Azure-wide unique, lowercase alphanumeric)."
  type        = string
}

variable "storage_container_name" {
  description = "Private blob container created for the application."
  type        = string
}

variable "account_tier" {
  description = "Storage account tier."
  type        = string
  default     = "Standard"
}

variable "account_replication_type" {
  description = "Replication type"
  type        = string
  default     = "LRS"
}

variable "blob_cors_allowed_origins" {
  description = "CORS allowed origins for Blob Storage."
  type        = list(string)
  default     = []
}
