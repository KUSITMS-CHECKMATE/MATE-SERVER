variable "resource_group_name" {
  description = "Resource Group name for the registry."
  type        = string
}

variable "location" {
  description = "Azure region for the registry."
  type        = string
}

variable "acr_name" {
  description = "Globally unique ACR name."
  type        = string
}

variable "acr_sku" {
  description = "ACR SKU: Basic (lowest cost), Standard, Premium."
  type        = string
  default     = "Basic"
}

variable "admin_enabled" {
  description = "Enable admin account (prefer false; use Managed Identity / AcrPull for AKS)."
  type        = bool
  default     = false
}
