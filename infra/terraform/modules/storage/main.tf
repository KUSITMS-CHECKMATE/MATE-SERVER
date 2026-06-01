resource "azurerm_storage_account" "this" {
  name                     = var.storage_account_name
  resource_group_name      = var.resource_group_name
  location                 = var.location
  account_tier             = var.account_tier
  account_replication_type = var.account_replication_type
}

resource "azurerm_storage_account_blob_service_properties" "this" {
  storage_account_id = azurerm_storage_account.this.id

  dynamic "cors_rule" {
    for_each = length(var.blob_cors_allowed_origins) > 0 ? [1] : []
    content {
      allowed_headers    = ["*"]
      allowed_methods    = ["GET", "PUT", "OPTIONS"]
      allowed_origins    = var.blob_cors_allowed_origins
      exposed_headers    = ["*"]
      max_age_in_seconds = 86400
    }
  }
}

resource "azurerm_storage_container" "app" {
  name                  = var.storage_container_name
  storage_account_name  = azurerm_storage_account.this.name
  container_access_type = "private"
}
