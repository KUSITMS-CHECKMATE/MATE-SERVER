package server.MATE.toss.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.crypto")
public record TossCryptoProperties(
        String decryptionKey,
        String aad,
        // 재가입 식별용 CI HMAC 키. 바꾸면 이미 저장된 users.ci_hash와 일치하지 않으므로 교체하면 안 된다.
        String ciHashKey
) {
}
