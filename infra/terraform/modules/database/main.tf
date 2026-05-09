resource "azurerm_private_dns_zone" "postgres" {
  name                = var.postgres_private_dns_zone_name
  resource_group_name = var.resource_group_name
}

resource "azurerm_private_dns_zone_virtual_network_link" "postgres" {
  name                  = var.postgres_private_dns_zone_link_name
  resource_group_name   = var.resource_group_name
  private_dns_zone_name = azurerm_private_dns_zone.postgres.name
  virtual_network_id    = var.virtual_network_id
}

resource "azurerm_postgresql_flexible_server" "this" {
  name                   = var.postgres_server_name
  resource_group_name    = var.resource_group_name
  location               = var.location
  version                = var.postgres_version
  delegated_subnet_id    = var.db_subnet_id
  private_dns_zone_id    = azurerm_private_dns_zone.postgres.id
  administrator_login    = var.postgres_admin_username
  administrator_password = var.postgres_admin_password
  sku_name               = var.postgres_sku_name
  storage_mb             = var.postgres_storage_mb
  backup_retention_days  = var.postgres_backup_retention_days

  public_network_access_enabled = false

  lifecycle {
    ignore_changes = [zone]
  }

  depends_on = [azurerm_private_dns_zone_virtual_network_link.postgres]
}

resource "azurerm_postgresql_flexible_server_database" "app" {
  name      = var.postgres_database_name
  server_id = azurerm_postgresql_flexible_server.this.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}
