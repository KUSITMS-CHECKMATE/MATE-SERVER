package server.MATE.domain.users.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TossAccountTest {

    @Test
    @DisplayName("markUnlinked 호출 시 tossUserKey와 scope까지 null로 처리한다")
    void markUnlinkedClearsIdentifyingFields() {
        Users user = Users.builder()
                .ci("ci-1")
                .name("tester")
                .role(Role.USER)
                .build();
        TossAccount tossAccount = TossAccount.builder()
                .user(user)
                .tossUserKey(777L)
                .encryptedTossRefreshToken("encrypted-refresh")
                .scope("user_ci")
                .isLinked(true)
                .lastLoginAt(LocalDateTime.of(2026, 5, 14, 12, 0))
                .lastTokenRefreshedAt(LocalDateTime.of(2026, 5, 14, 12, 0))
                .build();

        tossAccount.markUnlinked(TossUnlinkReferrer.UNLINK, LocalDateTime.of(2026, 5, 15, 0, 0));

        assertThat(tossAccount.isLinked()).isFalse();
        assertThat(tossAccount.getTossUserKey()).isNull();
        assertThat(tossAccount.getScope()).isNull();
        assertThat(tossAccount.getEncryptedTossRefreshToken()).isNull();
        assertThat(tossAccount.getUnlinkReferrer()).isEqualTo(TossUnlinkReferrer.UNLINK);
        assertThat(tossAccount.getUnlinkedAt()).isEqualTo(LocalDateTime.of(2026, 5, 15, 0, 0));
    }
}
