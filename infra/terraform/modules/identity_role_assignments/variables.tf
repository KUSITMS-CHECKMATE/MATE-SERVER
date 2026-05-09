variable "principal_id" {
  description = "Managed identity object (principal) id."
  type        = string
}

variable "key_vault_id" {
  description = "Key Vault resource id (scope for Secrets User)."
  type        = string
}

variable "storage_account_id" {
  description = "Storage Account resource id (scope for Blob Data Contributor)."
  type        = string
}

variable "assign_key_vault_secrets_user" {
  description = "Grant Key Vault Secrets User (list/get secrets)."
  type        = bool
  default     = true
}

variable "assign_storage_blob_data_contributor" {
  description = "Grant Storage Blob Data Contributor on the storage account."
  type        = bool
  default     = true
}

variable "acr_id" {
  description = "Azure Container Registry resource id (scope for AcrPull). Set null to skip."
  type        = string
  default     = null
}

variable "assign_acr_pull" {
  description = "Grant AcrPull on acr_id scope to the managed identity (required when ACR admin user is disabled)."
  type        = bool
  default     = true
}
