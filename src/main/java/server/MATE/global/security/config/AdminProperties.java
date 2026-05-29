package server.MATE.global.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mate.admin")
public record AdminProperties(
        String hardDeleteKey
) {
}
