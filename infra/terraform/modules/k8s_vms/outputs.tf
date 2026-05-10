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
  description = "control 공인 IP — SSH 및 kubectl(API server) 접근"
}

output "worker_private_ip_address" {
  value       = azurerm_network_interface.worker.private_ip_address
  description = "Worker 사설 IP (VPC 내부)"
}
