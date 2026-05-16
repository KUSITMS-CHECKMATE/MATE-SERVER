package server.MATE.domain.auth.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

class TokenEncryptorTest {

    private static final String VALID_BASE64_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    @DisplayName("encrypt/decrypt roundtrip이 동작한다")
    void encryptsAndDecryptsRoundTrip() {
        TokenEncryptor tokenEncryptor = new TokenEncryptor(new TokenEncryptionProperties(VALID_BASE64_KEY));

        String encrypted = tokenEncryptor.encrypt("refresh-token-value");
        String decrypted = tokenEncryptor.decrypt(encrypted);

        assertThat(encrypted).isNotEqualTo("refresh-token-value");
        assertThat(decrypted).isEqualTo("refresh-token-value");
    }

    @Test
    @DisplayName("잘못된 ciphertext는 AUTH_012 예외를 던진다")
    void throwsWhenCiphertextInvalid() {
        TokenEncryptor tokenEncryptor = new TokenEncryptor(new TokenEncryptionProperties(VALID_BASE64_KEY));

        assertThatThrownBy(() -> tokenEncryptor.decrypt("not-base64"))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(BaseErrorCode.AUTH_012);
    }

    @Test
    @DisplayName("key가 비어 있으면 AUTH_010 예외를 던진다")
    void throwsWhenKeyMissing() {
        assertThatThrownBy(() -> new TokenEncryptor(new TokenEncryptionProperties(" ")))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(BaseErrorCode.AUTH_010);
    }
}
