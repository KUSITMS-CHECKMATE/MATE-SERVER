output "control_vm_id" {
  value       = azurerm_linux_virtual_machine.control.id
  description = "Control plane VM resource id."
}

output "worker_vm_id" {
  value       = azurerm_linux_virtual_machine.worker.id
  description = "Worker VM resource id."
}

output "control_public_ip_address" {
  value       = azurerm_public_ip.control.ip_address
  description = "Control plane SSH / API access"
}

output "worker_public_ip_address" {
  value       = azurerm_public_ip.worker.ip_address
  description = "Worker public IP."
}
