package server.MATE.domain.users.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UsersTest {

    @Test
    @DisplayName("anonymizeForTossUnlink 호출 시 ci를 null로 처리한다")
    void anonymizeForTossUnlinkClearsCi() {
        Users user = Users.builder()
                .ci("ci-1")
                .name("tester")
                .role(Role.USER)
                .build();

        user.anonymizeForTossUnlink();

        assertThat(user.getCi()).isNull();
    }
}
