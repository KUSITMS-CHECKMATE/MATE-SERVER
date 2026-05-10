module "resource_group" {
  source = "../../modules/resource_group"

  resource_group_name = var.resource_group_name
}

module "networking" {
  source = "../../modules/networking"

  resource_group_name = module.resource_group.name
  location            = var.location

  vnet_name       = var.vnet_name
  vnet_cidr       = var.vnet_cidr
  app_subnet_name = var.app_subnet_name
  app_subnet_cidr = var.app_subnet_cidr
  db_subnet_name  = var.db_subnet_name
  db_subnet_cidr  = var.db_subnet_cidr
}

module "database" {
  source = "../../modules/database"

  resource_group_name = module.resource_group.name
  location            = var.location
  virtual_network_id  = module.networking.vnet_id
  db_subnet_id        = module.networking.db_subnet_id

  postgres_private_dns_zone_name      = var.postgres_private_dns_zone_name
  postgres_private_dns_zone_link_name = var.postgres_private_dns_zone_link_name
  postgres_server_name                = var.postgres_server_name
  postgres_database_name              = var.postgres_database_name
  postgres_admin_username             = var.postgres_admin_username
  postgres_admin_password             = var.postgres_admin_password
  postgres_version                    = var.postgres_version
  postgres_sku_name                   = var.postgres_sku_name
  postgres_storage_mb                 = var.postgres_storage_mb
  postgres_backup_retention_days      = var.postgres_backup_retention_days
}

module "storage" {
  source = "../../modules/storage"

  resource_group_name    = module.resource_group.name
  storage_account_name   = var.storage_account_name
  storage_container_name = var.storage_container_name
}

module "container_registry" {
  source = "../../modules/container_registry"

  resource_group_name = module.resource_group.name
  location            = var.location
  acr_name            = var.acr_name
  acr_sku             = var.acr_sku
  admin_enabled       = var.acr_admin_enabled
}

module "keyvault" {
  source = "../../modules/keyvault"

  resource_group_name      = module.resource_group.name
  location                 = var.location
  key_vault_name           = var.key_vault_name
  placeholder_secret_value = var.key_vault_placeholder_secret_value
  secret_initial_values    = var.key_vault_secret_initial_values
  deployer_object_id       = var.key_vault_deployer_object_id
}

module "user_assigned_identity" {
  source = "../../modules/user_assigned_identity"

  identity_name       = var.app_managed_identity_name
  resource_group_name = module.resource_group.name
  location            = var.location
}

module "identity_role_assignments" {
  source = "../../modules/identity_role_assignments"

  principal_id       = module.user_assigned_identity.principal_id
  key_vault_id       = module.keyvault.id
  storage_account_id = module.storage.storage_account_id

  assign_key_vault_secrets_user        = var.app_identity_assign_key_vault_secrets_user
  assign_storage_blob_data_contributor = var.app_identity_assign_storage_blob_data_contributor
  acr_id                               = module.container_registry.id
  assign_acr_pull                      = var.app_identity_assign_acr_pull
}

module "kubernetes_vms" {
  source = "../../modules/k8s_vms"

  resource_group_name = module.resource_group.name
  location            = var.location
  subnet_id           = module.networking.app_subnet_id

  admin_username  = var.kubernetes_vm_admin_username
  ssh_public_key  = var.kubernetes_vm_ssh_public_key
  control_vm_name = var.kubernetes_control_vm_name
  worker_vm_name  = var.kubernetes_worker_vm_name
}
