package server.MATE.toss.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.config.TossCryptoProperties;
import server.MATE.toss.dto.TossDecryptedUserInfo;
import server.MATE.toss.dto.TossLoginUserResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossUserInfoDecryptor {

    private static final int IV_LENGTH = 12;
    private static final int AUTH_TAG_LENGTH_BITS = 16 * Byte.SIZE;

    private final TossCryptoProperties tossCryptoProperties;

    public TossDecryptedUserInfo decrypt(TossLoginUserResponse response) {
        validateCryptoProperties();

        String ci = decryptRequired(response.ci(), BaseErrorCode.AUTH_008);

        return new TossDecryptedUserInfo(
                response.userKey(),
                response.scope(),
                ci,
                decryptOptional(response.name())
        );
    }

    private String decryptRequired(String encryptedValue, BaseErrorCode missingValueErrorCode) {
        String value = decryptOptional(encryptedValue);
        if (!StringUtils.hasText(value)) {
            log.warn("Toss decrypt required field missing after decrypt: sourcePresent={}, sourceLength={}",
                    StringUtils.hasText(encryptedValue),
                    encryptedValue == null ? null : encryptedValue.length());
            throw new BaseException(missingValueErrorCode);
        }
        return value;
    }

    private String decryptOptional(String encryptedValue) {
        if (!StringUtils.hasText(encryptedValue)) {
            return null;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedValue);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] keyBytes = Base64.getDecoder().decode(tossCryptoProperties.decryptionKey());
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec nonceSpec = new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv);

            cipher.init(Cipher.DECRYPT_MODE, key, nonceSpec);
            cipher.updateAAD(tossCryptoProperties.aad().getBytes(StandardCharsets.UTF_8));

            byte[] decrypted = cipher.doFinal(decoded, IV_LENGTH, decoded.length - IV_LENGTH);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Toss decrypt failed: sourceLength={}", encryptedValue.length(), e);
            throw new BaseException(BaseErrorCode.AUTH_007, BaseErrorCode.AUTH_007.getMessage(), e);
        }
    }

    private void validateCryptoProperties() {
        if (!StringUtils.hasText(tossCryptoProperties.decryptionKey()) || !StringUtils.hasText(tossCryptoProperties.aad())) {
            throw new BaseException(BaseErrorCode.AUTH_006);
        }
    }
}
