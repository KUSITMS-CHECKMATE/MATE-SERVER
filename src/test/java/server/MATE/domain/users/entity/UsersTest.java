package server.MATE.domain.users.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UsersTest {

    @Test
    @DisplayName("anonymizeForTossUnlink 호출 시 ci를 null로 처리하고, retainCiHash면 ciHash는 유지한다")
    void anonymizeForTossUnlinkClearsCiAndRetainsCiHash() {
        Users user = Users.builder()
                .ci("ci-1")
                .ciHash("ci-hash-1")
                .name("tester")
                .role(Role.USER)
                .build();

        user.anonymizeForTossUnlink(true);

        assertThat(user.getCi()).isNull();
        assertThat(user.getCiHash()).isEqualTo("ci-hash-1");
    }

    @Test
    @DisplayName("retainCiHash가 아니면 ciHash까지 null로 처리한다")
    void anonymizeForTossUnlinkClearsCiHashWhenNotRetained() {
        Users user = Users.builder()
                .ci("ci-1")
                .ciHash("ci-hash-1")
                .name("tester")
                .role(Role.USER)
                .build();

        user.anonymizeForTossUnlink(false);

        assertThat(user.getCi()).isNull();
        assertThat(user.getCiHash()).isNull();
    }
}
