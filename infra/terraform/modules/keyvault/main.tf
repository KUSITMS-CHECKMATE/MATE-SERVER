data "azurerm_client_config" "current" {}

locals {
  secret_slots = toset([
    "jwt-secret",
    "db-url",
    "db-username",
    "db-password",
    "discord-webhook-url",
    "azure-storage-connection-string",
    "azure-container-name",
  ])
}

resource "azurerm_key_vault" "this" {
  name                          = var.key_vault_name
  location                      = var.location
  resource_group_name           = var.resource_group_name
  tenant_id                     = data.azurerm_client_config.current.tenant_id
  sku_name                      = lower(var.sku_name)
  soft_delete_retention_days    = var.soft_delete_retention_days
  purge_protection_enabled      = var.purge_protection_enabled
  public_network_access_enabled = var.public_network_access_enabled
  rbac_authorization_enabled    = true
}

resource "azurerm_role_assignment" "deployer_secrets_officer" {
  scope                            = azurerm_key_vault.this.id
  role_definition_name             = "Key Vault Secrets Officer"
  principal_id                     = coalesce(var.deployer_object_id, data.azurerm_client_config.current.object_id)
  skip_service_principal_aad_check = true

  depends_on = [azurerm_key_vault.this]
}

resource "azurerm_key_vault_secret" "app" {
  for_each = local.secret_slots

  name         = each.key
  value        = lookup(var.secret_initial_values, each.key, var.placeholder_secret_value)
  key_vault_id = azurerm_key_vault.this.id

  lifecycle {
    ignore_changes = [value]
  }

  depends_on = [azurerm_role_assignment.deployer_secrets_officer]
}
