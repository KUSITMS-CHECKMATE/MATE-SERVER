locals {
  create_cloudflare_api_record = (
    var.cloudflare_zone_id != null &&
    trimspace(var.cloudflare_zone_id) != ""
  )
}

resource "cloudflare_record" "api" {
  count = local.create_cloudflare_api_record ? 1 : 0

  zone_id = trimspace(var.cloudflare_zone_id)
  name    = var.cloudflare_api_record_name
  type    = "A"
  value   = module.kubernetes_vms.control_public_ip_address
  ttl     = 1
  proxied = var.cloudflare_api_record_proxied

  comment = "MATE API ingress endpoint managed by Terraform"
}
