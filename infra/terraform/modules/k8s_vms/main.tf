locals {
  control_size = "Standard_B2s"
  worker_size  = "Standard_B2ms"
}

resource "azurerm_public_ip" "control" {
  name                = "${var.control_vm_name}-pip"
  location            = var.location
  resource_group_name = var.resource_group_name
  allocation_method   = "Static"
  sku                 = "Standard"

  tags = var.tags
}

resource "azurerm_public_ip" "worker" {
  name                = "${var.worker_vm_name}-pip"
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
    public_ip_address_id          = azurerm_public_ip.worker.id
  }

  tags = var.tags
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

  lifecycle {
    ignore_changes = [
      source_image_reference,
    ]
  }
}
