output "storage_account_name" {
  description = "Referenced Storage Account name."
  value       = data.azurerm_storage_account.this.name
}

output "storage_account_id" {
  description = "Referenced Storage Account resource id."
  value       = data.azurerm_storage_account.this.id
}

output "primary_blob_endpoint" {
  description = "Blob endpoint for the referenced Storage Account."
  value       = data.azurerm_storage_account.this.primary_blob_endpoint
}

output "storage_container_name" {
  description = "Blob container name used by the application."
  value       = var.storage_container_name
}
