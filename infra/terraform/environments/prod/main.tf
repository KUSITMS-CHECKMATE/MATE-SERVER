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
