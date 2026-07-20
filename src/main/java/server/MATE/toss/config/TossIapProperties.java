package server.MATE.toss.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.iap")
public record TossIapProperties(
        List<Tier> tiers
) {

    public TossIapProperties {
        if (tiers == null) {
            tiers = List.of();
        }
    }

    public record Tier(
            int goalPpl,
            int reward,
            String sku,
            int displayAmount
    ) {
    }
}
