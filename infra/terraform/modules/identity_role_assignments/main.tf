resource "azurerm_role_assignment" "key_vault_secrets_user" {
  count = var.assign_key_vault_secrets_user ? 1 : 0

  scope                            = var.key_vault_id
  role_definition_name             = "Key Vault Secrets User"
  principal_id                     = var.principal_id
  skip_service_principal_aad_check = true

  timeouts {
    create = "30m"
  }
}

resource "azurerm_role_assignment" "storage_blob_data_contributor" {
  count = var.assign_storage_blob_data_contributor ? 1 : 0

  scope                            = var.storage_account_id
  role_definition_name             = "Storage Blob Data Contributor"
  principal_id                     = var.principal_id
  skip_service_principal_aad_check = true

  timeouts {
    create = "30m"
  }
}

resource "azurerm_role_assignment" "acr_pull" {
  # acr_id는 (known after apply)일 수 있어 여기서 참조하면 count가 불안정해짐 → assign 플래그만 사용
  count = var.assign_acr_pull ? 1 : 0

  scope                            = var.acr_id
  role_definition_name             = "AcrPull"
  principal_id                     = var.principal_id
  skip_service_principal_aad_check = true

  timeouts {
    create = "30m"
  }
}
