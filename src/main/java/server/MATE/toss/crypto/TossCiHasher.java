package server.MATE.toss.crypto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// unlink 시 CI 원문은 지우되, 재가입한 동일인을 식별할 수 있도록 단방향 HMAC 값만 보관한다.
@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
public class TossCiHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static final int MIN_KEY_BYTES = 32;

    private final SecretKeySpec secretKeySpec;

    public TossCiHasher(TossCryptoProperties tossCryptoProperties) {
        String ciHashKey = tossCryptoProperties.ciHashKey();
        if (!StringUtils.hasText(ciHashKey)) {
            throw new IllegalStateException("toss.crypto.ci-hash-key 설정이 필요합니다.");
        }
        byte[] keyBytes = decodeKey(ciHashKey);
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("toss.crypto.ci-hash-key는 Base64 디코딩 기준 32바이트 이상이어야 합니다.");
        }
        this.secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    private byte[] decodeKey(String ciHashKey) {
        try {
            return Base64.getDecoder().decode(ciHashKey.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("toss.crypto.ci-hash-key는 Base64 형식이어야 합니다.", e);
        }
    }

    public String hash(String ci) {
        if (!StringUtils.hasText(ci)) {
            throw new IllegalArgumentException("CI가 비어 있습니다.");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(secretKeySpec);
            return HexFormat.of().formatHex(mac.doFinal(ci.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("CI 해시 생성에 실패했습니다.", e);
        }
    }
}
