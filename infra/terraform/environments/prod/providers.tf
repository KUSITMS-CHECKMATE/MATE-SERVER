terraform {
  required_version = ">= 1.6.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "~> 3.0"
    }
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = "~> 4.0"
    }
  }
}

provider "azurerm" {
  features {}

  resource_provider_registrations = "none"
}

# Entra Directory 읽기
provider "azuread" {}

# Cloudflare DNS. API token은 TF_VAR가 아니라 CLOUDFLARE_API_TOKEN 환경변수로 주입 권장.
provider "cloudflare" {}
