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

variable "placeholder_secret_value" {
  description = "Initial secret value until replaced in Portal/CLI (나중에 변경 필요)."
  type        = string
  default     = "PLACEHOLDER-SET-MANUALLY"
}

variable "secret_names" {
  description = "Key Vault에 만들 시크릿 이름 목록(슬롯), 환경·프로젝트별로 모듈 호출 시 주입(중복 없이)"
  type        = list(string)
  default = [
    "jwt-secret",
    "db-url",
    "db-username",
    "db-password",
    "discord-webhook-url",
    "azure-storage-connection-string",
    "azure-container-name",
    "toss-mtls-certificate",
    "toss-mtls-private-key",
  ]
}

variable "secret_initial_values" {
  description = "Optional map secret name -> value for first apply; later updates must be Portal/CLI (ignored by Terraform)."
  type        = map(string)
  default     = {}
  sensitive   = true
}

variable "deployer_object_id" {
  description = "Object ID for Key Vault Secrets Officer on this vault (default: terraform az login principal)."
  type        = string
  default     = null
}
