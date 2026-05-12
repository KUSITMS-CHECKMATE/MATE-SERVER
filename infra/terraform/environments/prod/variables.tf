variable "resource_group_name" {
  description = "Existing Resource Group name to reference only."
  type        = string
}

variable "location" {
  description = "Azure region used for deployable resources."
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

variable "postgres_server_name" {
  description = "PostgreSQL Flexible Server name."
  type        = string
}

variable "postgres_database_name" {
  description = "Application database name."
  type        = string
}

variable "postgres_admin_username" {
  description = "PostgreSQL admin username."
  type        = string
}

variable "postgres_admin_password" {
  description = "PostgreSQL admin password. Provide via TF_VAR_postgres_admin_password."
  type        = string
  sensitive   = true
}

variable "postgres_version" {
  description = "PostgreSQL engine version."
  type        = string
}

variable "postgres_sku_name" {
  description = "PostgreSQL Flexible Server SKU."
  type        = string
}

variable "postgres_storage_mb" {
  description = "PostgreSQL storage size in MB."
  type        = number
}

variable "postgres_backup_retention_days" {
  description = "PostgreSQL backup retention in days."
  type        = number
}

variable "storage_account_name" {
  description = "Storage Account name"
  type        = string
}

variable "storage_container_name" {
  description = "Private blob container name created inside the Storage Account."
  type        = string
}

variable "acr_name" {
  description = "Globally unique ACR name"
  type        = string
}

variable "acr_sku" {
  description = "Azure Container Registry SKU."
  type        = string
  default     = "Basic"
}

variable "acr_admin_enabled" {
  description = "If true, enables admin credentials (prefer false when using AcrPull)."
  type        = bool
  default     = false
}

variable "key_vault_name" {
  description = "Globally unique Key Vault name"
  type        = string
}

variable "key_vault_placeholder_secret_value" {
  description = "Placeholder for new secrets; replace real values in Portal (나중에 변경)."
  type        = string
  default     = "PLACEHOLDER-SET-MANUALLY"
}

variable "key_vault_secret_initial_values" {
  description = "Optional: set real values on first apply only (sensitive). Omit to use placeholder for all slots."
  type        = map(string)
  default     = {}
  sensitive   = true
}

variable "key_vault_deployer_object_id" {
  description = "Object ID to grant Key Vault Secrets Officer for Terraform (defaults to current login)."
  type        = string
  default     = null
}

variable "key_vault_deployer_principal_type" {
  description = "Secrets Officer 주체 유형: 로컬 az login(개인) = User, CI 서비스 프린시펄 = ServicePrincipal."
  type        = string
  default     = "User"
}

variable "app_managed_identity_name" {
  description = "User-assigned managed identity name (RG scope)."
  type        = string
}

variable "app_identity_assign_key_vault_secrets_user" {
  description = "Assign Key Vault Secrets User to app managed identity."
  type        = bool
  default     = true
}

variable "app_identity_assign_storage_blob_data_contributor" {
  description = "Assign Storage Blob Data Contributor to app managed identity."
  type        = bool
  default     = true
}

variable "app_identity_assign_acr_pull" {
  description = "Assign AcrPull on ACR for app MI"
  type        = bool
  default     = true
}

variable "kubernetes_vm_admin_username" {
  description = "SSH 사용자명 (Linux VM)."
  type        = string
  default     = "azureuser"
}

variable "kubernetes_vm_ssh_public_key" {
  description = "SSH 공개키. terraform.tfvars 또는 TF_VAR_kubernetes_vm_ssh_public_key 로 전달."
  type        = string
  sensitive   = true
}

variable "kubernetes_control_vm_name" {
  description = "K8s control 플레인 VM 이름 (SKU Standard_B2s)."
  type        = string
  default     = "mate-k8s-control"
}

variable "kubernetes_worker_vm_name" {
  description = "K8s worker VM 이름."
  type        = string
  default     = "mate-k8s-worker"
}

variable "kubernetes_control_vm_size" {
  description = "Control VM SKU."
  type        = string
  default     = "Standard_D2s_v3"
}

variable "kubernetes_worker_vm_size" {
  description = "Worker VM SKU. 재고 문제 시 Standard_D2s_v3 또는 리전 변경."
  type        = string
  default     = "Standard_B2ls_v2"
}

variable "kubernetes_ssh_allow_source_address_prefixes" {
  description = "K8s 노드 NSG: SSH(22) 허용 출발지"
  type        = list(string)
}

variable "kubernetes_api_allow_source_address_prefixes" {
  description = "K8s 노드 NSG: API(6443) 허용 출발지"
  type        = list(string)
}

