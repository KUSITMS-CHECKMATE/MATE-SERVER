locals {
  control_size = "Standard_B2s"
  worker_size  = "Standard_B2ms"
}

# 노드(VM) NIC 부착용 NSG — kube 구성표에 맞는 인바운드 기본 허용
resource "azurerm_network_security_group" "k8s" {
  name                = "${var.control_vm_name}-nodes-nsg"
  location            = var.location
  resource_group_name = var.resource_group_name

  tags = var.tags
}

resource "azurerm_network_security_rule" "ssh_in" {
  count = length(var.ssh_allow_source_address_prefixes)

  name                        = format("Inbound-SSH-22-%03d", count.index)
  priority                    = 1010 + count.index
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "22"
  destination_address_prefix  = "*"
  source_address_prefix       = var.ssh_allow_source_address_prefixes[count.index]
  resource_group_name         = var.resource_group_name
  network_security_group_name = azurerm_network_security_group.k8s.name
}

resource "azurerm_network_security_rule" "kube_api" {
  count = length(var.kubernetes_api_allow_source_address_prefixes)

  name                        = format("Inbound-Kube-API-TCP6443-%03d", count.index)
  priority                    = 1110 + count.index
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "6443"
  destination_address_prefix  = "*"
  source_address_prefix       = var.kubernetes_api_allow_source_address_prefixes[count.index]
  resource_group_name         = var.resource_group_name
  network_security_group_name = azurerm_network_security_group.k8s.name
}

resource "azurerm_network_security_rule" "etcd" {
  count = length(var.cluster_internal_allow_source_address_prefixes)

  name                        = format("Inbound-etcd-TCP2379-2380-%03d", count.index)
  priority                    = 1210 + count.index
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_ranges     = ["2379-2380"]
  destination_address_prefix  = "*"
  source_address_prefix       = var.cluster_internal_allow_source_address_prefixes[count.index]
  resource_group_name         = var.resource_group_name
  network_security_group_name = azurerm_network_security_group.k8s.name
}

resource "azurerm_network_security_rule" "kubelet" {
  count = length(var.cluster_internal_allow_source_address_prefixes)

  name                        = format("Inbound-kubelet-TCP10250-%03d", count.index)
  priority                    = 1310 + count.index
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "10250"
  destination_address_prefix  = "*"
  source_address_prefix       = var.cluster_internal_allow_source_address_prefixes[count.index]
  resource_group_name         = var.resource_group_name
  network_security_group_name = azurerm_network_security_group.k8s.name
}

resource "azurerm_network_security_rule" "kube_scheduler" {
  count = length(var.cluster_internal_allow_source_address_prefixes)

  name                        = format("Inbound-kube-sched-TCP10251-%03d", count.index)
  priority                    = 1410 + count.index
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "10251"
  destination_address_prefix  = "*"
  source_address_prefix       = var.cluster_internal_allow_source_address_prefixes[count.index]
  resource_group_name         = var.resource_group_name
  network_security_group_name = azurerm_network_security_group.k8s.name
}

resource "azurerm_network_security_rule" "kube_controller_manager" {
  count = length(var.cluster_internal_allow_source_address_prefixes)

  name                        = format("Inbound-kube-ctrl-mgr-TCP10252-%03d", count.index)
  priority                    = 1510 + count.index
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "10252"
  destination_address_prefix  = "*"
  source_address_prefix       = var.cluster_internal_allow_source_address_prefixes[count.index]
  resource_group_name         = var.resource_group_name
  network_security_group_name = azurerm_network_security_group.k8s.name
}

resource "azurerm_network_security_rule" "nodeport" {
  count = length(var.nodeport_allow_source_address_prefixes)

  name                        = format("Inbound-NodePort-%03d", count.index)
  priority                    = 1610 + count.index
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_ranges     = ["30000-32767"]
  destination_address_prefix  = "*"
  source_address_prefix       = var.nodeport_allow_source_address_prefixes[count.index]
  resource_group_name         = var.resource_group_name
  network_security_group_name = azurerm_network_security_group.k8s.name
}

# 마스터(control) 접속·kubectl(API server) 접근용 공인 IP 1개만 사용
resource "azurerm_public_ip" "control" {
  name                = "${var.control_vm_name}-pip"
  location            = var.location
  resource_group_name = var.resource_group_name
  allocation_method   = "Static"
  sku                 = "Standard"

  tags = var.tags
}

resource "azurerm_network_interface" "control" {
  name                = "${var.control_vm_name}-nic"
  location            = var.location
  resource_group_name = var.resource_group_name

  ip_configuration {
    name                          = "primary"
    subnet_id                     = var.subnet_id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.control.id
  }

  tags = var.tags
}

resource "azurerm_network_interface" "worker" {
  name                = "${var.worker_vm_name}-nic"
  location            = var.location
  resource_group_name = var.resource_group_name

  ip_configuration {
    name                          = "primary"
    subnet_id                     = var.subnet_id
    private_ip_address_allocation = "Dynamic"
    # 워커는 사설만 (마스터 경유 SSH / 클러스터 내부 통신)
  }

  tags = var.tags
}

resource "azurerm_network_interface_security_group_association" "control" {
  network_interface_id      = azurerm_network_interface.control.id
  network_security_group_id = azurerm_network_security_group.k8s.id
}

resource "azurerm_network_interface_security_group_association" "worker" {
  network_interface_id      = azurerm_network_interface.worker.id
  network_security_group_id = azurerm_network_security_group.k8s.id
}

resource "azurerm_linux_virtual_machine" "control" {
  name                = var.control_vm_name
  resource_group_name = var.resource_group_name
  location            = var.location
  size                = local.control_size
  admin_username      = var.admin_username

  disable_password_authentication = true

  admin_ssh_key {
    username   = var.admin_username
    public_key = var.ssh_public_key
  }

  network_interface_ids = [
    azurerm_network_interface.control.id,
  ]

  os_disk {
    name                 = "${var.control_vm_name}-os"
    caching              = "ReadWrite"
    storage_account_type = var.os_disk_storage_account_type
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "0001-com-ubuntu-server-jammy"
    sku       = "22_04-lts-gen2"
    version   = "latest"
  }

  tags = var.tags

  depends_on = [azurerm_network_interface_security_group_association.control]

  lifecycle {
    ignore_changes = [
      source_image_reference,
    ]
  }
}

resource "azurerm_linux_virtual_machine" "worker" {
  name                = var.worker_vm_name
  resource_group_name = var.resource_group_name
  location            = var.location
  size                = local.worker_size
  admin_username      = var.admin_username

  disable_password_authentication = true

  admin_ssh_key {
    username   = var.admin_username
    public_key = var.ssh_public_key
  }

  network_interface_ids = [
    azurerm_network_interface.worker.id,
  ]

  os_disk {
    name                 = "${var.worker_vm_name}-os"
    caching              = "ReadWrite"
    storage_account_type = var.os_disk_storage_account_type
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "0001-com-ubuntu-server-jammy"
    sku       = "22_04-lts-gen2"
    version   = "latest"
  }

  tags = var.tags

  depends_on = [azurerm_network_interface_security_group_association.worker]

  lifecycle {
    ignore_changes = [
      source_image_reference,
    ]
  }
}
