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
  value   = module.kubernetes_vms.ingress_public_ip_address
  ttl     = 1
  proxied = var.cloudflare_api_record_proxied

  comment = "MATE API ingress LB endpoint managed by Terraform"
}

resource "cloudflare_record" "grafana" {
  count = local.create_cloudflare_api_record ? 1 : 0

  zone_id = trimspace(var.cloudflare_zone_id)
  name    = var.cloudflare_grafana_record_name
  type    = "A"
  value   = module.kubernetes_vms.ingress_public_ip_address
  ttl     = 1
  proxied = var.cloudflare_api_record_proxied

  comment = "Grafana ingress LB endpoint managed by Terraform"
}

resource "cloudflare_record" "argocd" {
  count = local.create_cloudflare_api_record ? 1 : 0

  zone_id = trimspace(var.cloudflare_zone_id)
  name    = var.cloudflare_argocd_record_name
  type    = "A"
  value   = module.kubernetes_vms.ingress_public_ip_address
  ttl     = 1
  proxied = var.cloudflare_api_record_proxied

  comment = "ArgoCD ingress LB endpoint managed by Terraform"
}
