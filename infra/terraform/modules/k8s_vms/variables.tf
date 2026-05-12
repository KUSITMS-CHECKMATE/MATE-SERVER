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

variable "control_vm_size" {
  description = "Control plane VM SKU. DASv5 패밀리는 구독·리전별 코어 할당량 0 일 수 있음 → D2s_v3 등 DSv3으로 대체ㅠㅠ"
  type        = string
  default     = "Standard_D2s_v3"
}

variable "worker_vm_size" {
  description = "Worker VM SKU. Southeast Asia 등에서 B2s 재고 부족 시 B2ls_v2 또는 더 작은 B로 시도."
  type        = string
  default     = "Standard_B2ls_v2"
}

variable "ssh_allow_source_address_prefixes" {
  description = "SSH(22) 인바운드 허용 출발지"
  type        = list(string)
  default     = []
}

variable "kubernetes_api_allow_source_address_prefixes" {
  description = "API(6443) 인바운드 허용 출발지"
  type        = list(string)
  default     = []
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
