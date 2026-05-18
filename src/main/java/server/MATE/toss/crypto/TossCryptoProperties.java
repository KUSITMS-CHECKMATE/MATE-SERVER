package server.MATE.toss.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.crypto")
public record TossCryptoProperties(
        String decryptionKey,
        String aad
) {
}
