module "resource_group" {
  source = "../../modules/resource_group"

  resource_group_name = var.resource_group_name
}

module "networking" {
  source = "../../modules/networking"

  resource_group_name = module.resource_group.name
  location            = module.resource_group.location

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
  location            = module.resource_group.location
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
