package server.MATE.domain.auth.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Component
public class TokenEncryptor {

    private static final int IV_LENGTH = 12;
    private static final int AUTH_TAG_LENGTH_BITS = 16 * Byte.SIZE;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec secretKeySpec;

    public TokenEncryptor(TokenEncryptionProperties tokenEncryptionProperties) {
        this.secretKeySpec = initializeSecretKeySpec(tokenEncryptionProperties);
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv));

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
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv));

            byte[] decrypted = cipher.doFinal(decoded, IV_LENGTH, decoded.length - IV_LENGTH);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(BaseErrorCode.AUTH_012, BaseErrorCode.AUTH_012.getMessage(), e);
        }
    }

    private SecretKeySpec initializeSecretKeySpec(TokenEncryptionProperties tokenEncryptionProperties) {
        if (!StringUtils.hasText(tokenEncryptionProperties.key())) {
            throw new BaseException(BaseErrorCode.AUTH_010);
        }

        try {
            byte[] decodedKey = Base64.getDecoder().decode(tokenEncryptionProperties.key());
            return new SecretKeySpec(decodedKey, "AES");
        } catch (Exception e) {
            throw new BaseException(BaseErrorCode.AUTH_011, BaseErrorCode.AUTH_011.getMessage(), e);
        }
    }
}
