output "id" {
  description = "Key Vault resource id."
  value       = azurerm_key_vault.this.id
}

output "name" {
  description = "Key Vault name."
  value       = azurerm_key_vault.this.name
}

output "vault_uri" {
  description = "DNS URI for the vault."
  value       = azurerm_key_vault.this.vault_uri
}

output "secret_names" {
  description = "Application secret slots created in Key Vault."
  value       = sort(keys(azurerm_key_vault_secret.app))
}

output "env_var_mapping" {
  description = "Key Vault secret name -> Spring env (application-prod.yml)."
  value = {
    "jwt-secret"                      = "JWT_SECRET"
    "db-url"                          = "DB_URL"
    "db-username"                     = "DB_USERNAME"
    "db-password"                     = "DB_PASSWORD"
    "discord-webhook-url"             = "DISCORD_WEBHOOK_URL"
    "azure-storage-connection-string" = "AZURE_CONNECTION"
    "azure-container-name"            = "AZURE_CONTAINER_NAME"
    # Spring ssl.bundle.pem.toss.keystore file paths
    "toss-mtls-certificate" = "TOSS_MTLS_CERT_PATH (PEM file content from KV → write to disk → path here)"
    "toss-mtls-private-key" = "TOSS_MTLS_KEY_PATH (PEM file content from KV → write to disk → path here)"
  }
}
