package server.MATE.toss.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.dto.response.TossDecryptedUserInfo;
import server.MATE.toss.dto.response.TossLoginUserResponse;

class TossUserInfoDecryptorTest {

    private static final String VALID_BASE64_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String VALID_AAD = "test-aad";
    private static final int IV_LENGTH = 12;
    private static final int AUTH_TAG_LENGTH_BITS = 16 * Byte.SIZE;

    @Test
    @DisplayName("ci/name 복호화가 정상 동작한다")
    void decryptsCiAndNameSuccessfully() throws Exception {
        TossUserInfoDecryptor decryptor = new TossUserInfoDecryptor(new TossCryptoProperties(VALID_BASE64_KEY, VALID_AAD));
        TossLoginUserResponse response = new TossLoginUserResponse(
                777L,
                "user_ci,name",
                encrypt("tester"),
                encrypt("ci-value")
        );

        TossDecryptedUserInfo decrypted = decryptor.decrypt(response);

        assertThat(decrypted.userKey()).isEqualTo(777L);
        assertThat(decrypted.scope()).isEqualTo("user_ci,name");
        assertThat(decrypted.name()).isEqualTo("tester");
        assertThat(decrypted.ci()).isEqualTo("ci-value");
    }

    @Test
    @DisplayName("ci 복호화 결과가 비어 있으면 AUTH_008 예외를 던진다")
    void throwsWhenCiMissingAfterDecrypt() {
        TossUserInfoDecryptor decryptor = new TossUserInfoDecryptor(new TossCryptoProperties(VALID_BASE64_KEY, VALID_AAD));
        TossLoginUserResponse response = new TossLoginUserResponse(777L, "user_ci", encryptQuietly("tester"), null);

        assertThatThrownBy(() -> decryptor.decrypt(response))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(BaseErrorCode.AUTH_008);
    }

    @Test
    @DisplayName("잘못된 암호문은 AUTH_007 예외를 던진다")
    void throwsWhenCipherTextInvalid() {
        TossUserInfoDecryptor decryptor = new TossUserInfoDecryptor(new TossCryptoProperties(VALID_BASE64_KEY, VALID_AAD));
        TossLoginUserResponse response = new TossLoginUserResponse(777L, "user_ci", "not-base64", encryptQuietly("ci-value"));

        assertThatThrownBy(() -> decryptor.decrypt(response))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(BaseErrorCode.AUTH_007);
    }

    @Test
    @DisplayName("필수 암복호화 설정이 없으면 AUTH_006 예외를 던진다")
    void throwsWhenCryptoPropertiesMissing() {
        assertThatThrownBy(() -> new TossUserInfoDecryptor(new TossCryptoProperties(null, null)))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(BaseErrorCode.AUTH_006);
    }

    private String encryptQuietly(String plainText) {
        try {
            return encrypt(plainText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String encrypt(String plainText) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(VALID_BASE64_KEY);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv));
        cipher.updateAAD(VALID_AAD.getBytes(StandardCharsets.UTF_8));

        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return Base64.getEncoder().encodeToString(combined);
    }
}
