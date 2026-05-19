resource "azurerm_redis_cache" "this" {
  name                = var.redis_cache_name
  location            = var.location
  resource_group_name = var.resource_group_name

  capacity                      = var.capacity
  family                        = var.family
  sku_name                      = var.sku_name
  minimum_tls_version           = var.minimum_tls_version
  non_ssl_port_enabled          = false
  public_network_access_enabled = false


  redis_configuration {
  }

  tags = var.tags
}

resource "azurerm_private_dns_zone" "redis" {
  name                = var.private_dns_zone_name
  resource_group_name = var.resource_group_name
}

resource "azurerm_private_dns_zone_virtual_network_link" "redis" {
  name                  = var.private_dns_zone_link_name
  resource_group_name   = var.resource_group_name
  private_dns_zone_name = azurerm_private_dns_zone.redis.name
  virtual_network_id    = var.virtual_network_id
}

resource "azurerm_private_endpoint" "redis" {
  name                = "${var.redis_cache_name}-pe"
  location            = var.location
  resource_group_name = var.resource_group_name
  subnet_id           = var.private_endpoint_subnet_id

  private_service_connection {
    name                           = "${var.redis_cache_name}-psc"
    private_connection_resource_id = azurerm_redis_cache.this.id
    subresource_names              = ["redisCache"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "redis-private-dns-zone-group"
    private_dns_zone_ids = [azurerm_private_dns_zone.redis.id]
  }

  depends_on = [azurerm_private_dns_zone_virtual_network_link.redis]
}
