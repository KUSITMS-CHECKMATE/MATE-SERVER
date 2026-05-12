output "storage_account_name" {
  description = "Storage Account name."
  value       = azurerm_storage_account.this.name
}

output "storage_account_id" {
  description = "Storage Account resource id."
  value       = azurerm_storage_account.this.id
}

output "primary_blob_endpoint" {
  description = "Primary blob endpoint for the Storage Account."
  value       = azurerm_storage_account.this.primary_blob_endpoint
}

output "storage_container_name" {
  description = "Blob container name used by the application."
  value       = azurerm_storage_container.app.name
}
