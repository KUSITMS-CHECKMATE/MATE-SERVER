variable "resource_group_name" {
  description = "Resource group hosting the VMs."
  type        = string
}

variable "location" {
  description = "Azure region."
  type        = string
}

variable "subnet_id" {
  description = "Subnet id for NICs (e.g. app subnet)."
  type        = string
}

variable "admin_username" {
  description = "Linux admin username (SSH)."
  type        = string
}

variable "ssh_public_key" {
  description = "SSH public key for admin access."
  type        = string
  sensitive   = true
}

variable "control_vm_name" {
  description = "Kubernetes control plane VM name."
  type        = string
}

variable "worker_vm_name" {
  description = "Kubernetes worker VM name."
  type        = string
}

variable "os_disk_storage_account_type" {
  description = "OS managed disk SKU."
  type        = string
  default     = "Standard_LRS"
}

variable "tags" {
  description = "Optional resource tags."
  type        = map(string)
  default     = {}
}

variable "ssh_allow_source_address_prefixes" {
  description = "SSH(22) 인바운드 허용 출발지 목록"
  type        = list(string)
  default     = ["*"]
}

variable "kubernetes_api_allow_source_address_prefixes" {
  description = "API 서버(6443) 허용 출발지 목록 (* = 인터넷; 내부만이면 VirtualNetwork 또는 CIDR)."
  type        = list(string)
  default     = ["*"]
}

variable "cluster_internal_allow_source_address_prefixes" {
  description = "etcd/스케줄러/컨트롤러/kubelet 포트 허용 출발지. 기본 VNet 내."
  type        = list(string)
  default     = ["VirtualNetwork"]
}

variable "nodeport_allow_source_address_prefixes" {
  description = "NodePort(30000-32767) 허용 출발지. 필요 시 * 로 인터넷 노출 가능."
  type        = list(string)
  default     = ["VirtualNetwork"]
}
