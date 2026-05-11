output "key_vault_secrets_user_assignment_id" {
  description = "Role assignment id for Key Vault Secrets User"
  value       = try(azurerm_role_assignment.key_vault_secrets_user[0].id, null)
}

output "storage_blob_data_contributor_assignment_id" {
  description = "Role assignment id for Storage Blob Data Contributor"
  value       = try(azurerm_role_assignment.storage_blob_data_contributor[0].id, null)
}

output "acr_pull_assignment_id" {
  description = "Role assignment id for AcrPull (if created)"
  value       = try(azurerm_role_assignment.acr_pull[0].id, null)
}
