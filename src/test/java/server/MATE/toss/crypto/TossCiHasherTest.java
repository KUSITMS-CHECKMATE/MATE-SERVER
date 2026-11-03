package server.MATE.toss.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TossCiHasherTest {

    private static final String VALID_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    @DisplayName("같은 CI는 항상 같은 64자리 hex 해시를 만들고, 다른 CI나 다른 키는 다른 해시를 만든다")
    void hashesDeterministically() {
        TossCiHasher hasher = hasher(VALID_KEY);
        TossCiHasher otherKeyHasher = hasher("ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=");

        String hash = hasher.hash("ci-1");

        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
        assertThat(hasher.hash("ci-1")).isEqualTo(hash);
        assertThat(hasher.hash("ci-2")).isNotEqualTo(hash);
        assertThat(otherKeyHasher.hash("ci-1")).isNotEqualTo(hash);
    }

    @Test
    @DisplayName("키가 없거나 Base64가 아니거나 32바이트보다 짧으면 생성에 실패한다")
    void rejectsInvalidKey() {
        assertThatThrownBy(() -> hasher("")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> hasher("not-base64!!")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
        assertThatThrownBy(() -> hasher("c2hvcnQta2V5")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("빈 CI는 해시하지 않는다")
    void rejectsBlankCi() {
        TossCiHasher hasher = hasher(VALID_KEY);

        assertThatThrownBy(() -> hasher.hash(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    private TossCiHasher hasher(String ciHashKey) {
        return new TossCiHasher(new TossCryptoProperties(null, null, ciHashKey));
    }
}
