variable "resource_group_name" {
  description = "Resource group hosting the VMs."
  type        = string
}

variable "location" {
  description = "Azure region."
  type        = string
}

variable "subnet_id" {
  description = "Subnet id for NICs (e.g. app subnet)."
  type        = string
}

variable "admin_username" {
  description = "Linux admin username (SSH)."
  type        = string
}

variable "ssh_public_key" {
  description = "SSH public key for admin access."
  type        = string
  sensitive   = true
}

variable "control_vm_name" {
  description = "Kubernetes control plane VM name."
  type        = string
}

variable "worker_vm_name" {
  description = "Kubernetes worker VM name."
  type        = string
}

variable "os_disk_storage_account_type" {
  description = "OS managed disk SKU."
  type        = string
  default     = "Standard_LRS"
}

variable "tags" {
  description = "Optional resource tags."
  type        = map(string)
  default     = {}
}
