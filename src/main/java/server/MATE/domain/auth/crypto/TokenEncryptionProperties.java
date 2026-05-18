package server.MATE.domain.auth.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.token-encryption")
public record TokenEncryptionProperties(
        String key
) {
}
