variable "resource_group_name" {
  description = "Resource Group that contains the existing Storage Account."
  type        = string
}

variable "storage_account_name" {
  description = "Existing Storage Account name to reference only."
  type        = string
}

variable "storage_container_name" {
  description = "Blob container name used by the application."
  type        = string
}
