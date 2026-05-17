locals {
  # Spring(application-prod.yml)전용 — Key Vault 슬롯 이름 → 실행 시 넣어줄 환경 변수/설명
  spring_keyvault_env_mapping = {
    "jwt-secret"                      = "JWT_SECRET"
    "db-url"                          = "DB_URL"
    "db-username"                     = "DB_USERNAME"
    "db-password"                     = "DB_PASSWORD"
    "discord-webhook-url"             = "DISCORD_WEBHOOK_URL"
    "azure-storage-connection-string" = "AZURE_CONNECTION"
    "azure-container-name"            = "AZURE_CONTAINER_NAME"
    # Spring ssl.bundle.pem.toss.keystore 는 파일 경로 전제 (KV PEM → 디스크 반영 후 path)
    "toss-mtls-certificate" = "TOSS_MTLS_CERT_PATH (PEM file content from KV → write to disk → path here)"
    "toss-mtls-private-key" = "TOSS_MTLS_KEY_PATH (PEM file content from KV → write to disk → path here)"
  }
}

output "resource_group_id" {
  description = "Referenced existing Resource Group id."
  value       = module.resource_group.id
}

output "resource_group_name" {
  description = "Referenced existing Resource Group name."
  value       = module.resource_group.name
}

output "resource_group_location" {
  description = "Referenced existing Resource Group location."
  value       = module.resource_group.location
}

output "vnet_id" {
  description = "Created Virtual Network id."
  value       = module.networking.vnet_id
}

output "app_subnet_id" {
  description = "Application subnet id for AKS."
  value       = module.networking.app_subnet_id
}

output "db_subnet_id" {
  description = "Database subnet id for PostgreSQL."
  value       = module.networking.db_subnet_id
}

output "postgres_private_dns_zone_id" {
  description = "PostgreSQL private DNS zone id."
  value       = module.database.postgres_private_dns_zone_id
}

output "postgres_private_dns_zone_link_id" {
  description = "PostgreSQL private DNS zone VNet link id."
  value       = module.database.postgres_private_dns_zone_link_id
}

output "postgres_server_id" {
  description = "PostgreSQL Flexible Server id."
  value       = module.database.postgres_server_id
}

output "postgres_server_fqdn" {
  description = "PostgreSQL Flexible Server FQDN."
  value       = module.database.postgres_server_fqdn
}

output "postgres_database_id" {
  description = "PostgreSQL application database id."
  value       = module.database.postgres_database_id
}

output "storage_account_name" {
  description = "Referenced Storage Account name."
  value       = module.storage.storage_account_name
}

output "storage_blob_endpoint" {
  description = "Blob endpoint for the referenced Storage Account."
  value       = module.storage.primary_blob_endpoint
}

output "storage_container_name" {
  description = "Blob container name used by the application."
  value       = module.storage.storage_container_name
}

output "acr_id" {
  description = "Azure Container Registry resource id."
  value       = module.container_registry.id
}

output "acr_name" {
  description = "Azure Container Registry name."
  value       = module.container_registry.name
}

output "acr_login_server" {
  description = "ACR login server (image host)."
  value       = module.container_registry.login_server
}

output "key_vault_id" {
  description = "Key Vault resource id."
  value       = module.keyvault.id
}

output "key_vault_name" {
  description = "Key Vault name."
  value       = module.keyvault.name
}

output "key_vault_uri" {
  description = "Key Vault URI."
  value       = module.keyvault.vault_uri
}

output "key_vault_secret_names" {
  description = "Created application secret slots (update values in Portal)."
  value       = module.keyvault.secret_names
}

output "key_vault_env_var_mapping" {
  description = "KV secret name -> Spring env (application-prod.yml)."
  value = {
    for k, v in local.spring_keyvault_env_mapping :
    k => v if contains(module.keyvault.secret_names, k)
  }
}

output "app_managed_identity_id" {
  description = "User-assigned managed identity resource id."
  value       = module.user_assigned_identity.id
}

output "app_managed_identity_principal_id" {
  description = "User-assigned identity principal id (for RBAC role assignments)."
  value       = module.user_assigned_identity.principal_id
}

output "app_managed_identity_client_id" {
  description = "User-assigned identity client id (for SDK / federation)."
  value       = module.user_assigned_identity.client_id
}

output "app_managed_identity_tenant_id" {
  description = "Tenant id for the managed identity."
  value       = module.user_assigned_identity.tenant_id
}

output "app_identity_kv_secrets_user_role_assignment_id" {
  description = "Role assignment: MI -> Key Vault (Secrets User)."
  value       = module.identity_role_assignments.key_vault_secrets_user_assignment_id
}

output "app_identity_storage_blob_contributor_role_assignment_id" {
  description = "Role assignment: MI -> Storage Account (Blob Data Contributor)."
  value       = module.identity_role_assignments.storage_blob_data_contributor_assignment_id
}

output "app_identity_acr_pull_role_assignment_id" {
  description = "Role assignment: MI -> ACR (AcrPull)."
  value       = module.identity_role_assignments.acr_pull_assignment_id
}

output "storage_account_id" {
  description = "Referenced Storage Account id (RBAC scope)."
  value       = module.storage.storage_account_id
}

output "kubernetes_control_vm_id" {
  description = "K8s control plane Linux VM resource id."
  value       = module.kubernetes_vms.control_vm_id
}

output "kubernetes_worker_vm_id" {
  description = "K8s worker Linux VM resource id."
  value       = module.kubernetes_vms.worker_vm_id
}

output "kubernetes_control_public_ip" {
  description = "마스터(control) 공인 IP — SSH 및 kubectl(API server)."
  value       = module.kubernetes_vms.control_public_ip_address
}

output "kubernetes_worker_private_ip" {
  description = "K8s worker 사설 IP"
  value       = module.kubernetes_vms.worker_private_ip_address
}

output "kubernetes_nodes_nsg_id" {
  description = "K8s 노드(VM) NIC에 부착한 NSG 리소스 id."
  value       = module.kubernetes_vms.network_security_group_id
}

output "github_actions_acr_push_role_assignment_id" {
  description = "GitHub Actions OIDC SP에 부여한 AcrPush 역할 할당 id. github_actions_oidc_application_client_id 미설정 시 null."
  value       = try(azurerm_role_assignment.github_actions_acr_push[0].id, null)
}
