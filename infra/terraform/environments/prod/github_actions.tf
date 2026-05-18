locals {
  grant_github_actions_acr_push = (
    var.github_actions_oidc_application_client_id != null &&
    trimspace(var.github_actions_oidc_application_client_id) != ""
  )
}

data "azuread_service_principal" "github_actions" {
  count = local.grant_github_actions_acr_push ? 1 : 0

  client_id = trimspace(var.github_actions_oidc_application_client_id)
}

resource "azurerm_role_assignment" "github_actions_acr_push" {
  count = local.grant_github_actions_acr_push ? 1 : 0

  scope                            = module.container_registry.id
  role_definition_name             = "AcrPush"
  principal_id                     = data.azuread_service_principal.github_actions[0].object_id
  principal_type                   = "ServicePrincipal"
  skip_service_principal_aad_check = true

  timeouts {
    create = "30m"
  }
}
