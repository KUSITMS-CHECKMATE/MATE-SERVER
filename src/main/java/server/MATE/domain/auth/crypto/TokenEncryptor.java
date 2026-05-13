package server.MATE.domain.auth.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Component
@RequiredArgsConstructor
public class TokenEncryptor {

    private static final int IV_LENGTH = 12;
    private static final int AUTH_TAG_LENGTH_BITS = 16 * Byte.SIZE;

    private final TokenEncryptionProperties tokenEncryptionProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String plainText) {
        validateEncryptionKey();

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec key = new SecretKeySpec(Base64.getDecoder().decode(tokenEncryptionProperties.key()), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(BaseErrorCode.AUTH_011, BaseErrorCode.AUTH_011.getMessage(), e);
        }
    }

    public String decrypt(String encryptedText) {
        validateEncryptionKey();

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec key = new SecretKeySpec(Base64.getDecoder().decode(tokenEncryptionProperties.key()), "AES");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv));

            byte[] decrypted = cipher.doFinal(decoded, IV_LENGTH, decoded.length - IV_LENGTH);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(BaseErrorCode.AUTH_012, BaseErrorCode.AUTH_012.getMessage(), e);
        }
    }

    private void validateEncryptionKey() {
        if (!StringUtils.hasText(tokenEncryptionProperties.key())) {
            throw new BaseException(BaseErrorCode.AUTH_010);
        }
    }
}
