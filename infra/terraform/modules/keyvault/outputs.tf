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
