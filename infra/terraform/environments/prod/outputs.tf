output "resource_group_id" {
  description = "Referenced existing Resource Group id."
  value       = module.resource_group.id
}

output "resource_group_name" {
  description = "Referenced existing Resource Group name."
  value       = module.resource_group.name
}

output "resource_group_location" {
  description = "Referenced existing Resource Group location."
  value       = module.resource_group.location
}
