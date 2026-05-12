resource_group_name = "kusitms_mate"
location            = "southeastasia"

vnet_name = "mate-vnet"
vnet_cidr = "10.20.0.0/16"

app_subnet_name = "snet-aks"
app_subnet_cidr = "10.20.1.0/24"

db_subnet_name = "snet-postgres"
db_subnet_cidr = "10.20.2.0/24"

postgres_private_dns_zone_name      = "privatelink.postgres.database.azure.com"
postgres_private_dns_zone_link_name = "mate-postgres-private-dns-link"

postgres_server_name           = "mate-postgres"
postgres_database_name         = "mate"
postgres_admin_username        = "mateadmin"
postgres_version               = "16"
postgres_sku_name              = "B_Standard_B2s"
postgres_storage_mb            = 32768
postgres_backup_retention_days = 7

storage_account_name   = "matestoragedev"
storage_container_name = "mate-images"

# ACR 이름은 Azure 전역에서 유일하도록
acr_name = "kusitmsmateacr"
acr_sku  = "Basic"

# Key Vault 이름
key_vault_name = "kusitms-mate-keyvault"

# 앱(VM)에 부착할 User-assigned MI 이름 
app_managed_identity_name = "mate-app-identity"

kubernetes_vm_admin_username = "azureuser"
kubernetes_vm_ssh_public_key = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIHCcRTcz5A5nuzXRwadCiwiTC+5qLDx9e9IxY3Dog2EQ mate-k8s"
kubernetes_control_vm_name   = "mate-k8s-control"
kubernetes_worker_vm_name    = "mate-k8s-worker"

kubernetes_control_vm_size = "Standard_D2s_v3"
kubernetes_worker_vm_size  = "Standard_B2ls_v2"

# NSG: 이 PC 공인 기준 허용(동적 IP면 바뀔 때마다 수정 후 apply).
kubernetes_ssh_allow_source_address_prefixes = ["175.118.225.161/32"]
kubernetes_api_allow_source_address_prefixes = ["175.118.225.161/32"]

