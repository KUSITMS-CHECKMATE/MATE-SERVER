resource_group_name = "kusitms_mate"

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
postgres_sku_name              = "B_Standard_B1ms"
postgres_storage_mb            = 32768
postgres_backup_retention_days = 7
