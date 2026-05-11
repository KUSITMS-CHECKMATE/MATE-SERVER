variable "identity_name" {
  description = "User-assigned managed identity name."
  type        = string
}

variable "resource_group_name" {
  description = "Resource Group for the identity."
  type        = string
}

variable "location" {
  description = "Azure region."
  type        = string
}
