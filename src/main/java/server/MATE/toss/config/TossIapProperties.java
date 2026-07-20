package server.MATE.toss.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.iap")
public record TossIapProperties(
        List<String> allowedSkus
) {

    public TossIapProperties {
        if (allowedSkus == null) {
            allowedSkus = List.of();
        }
    }
}
