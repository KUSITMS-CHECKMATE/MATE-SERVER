package server.MATE.global.discord.outbox.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiscordOutboxTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 24, 10, 0, 0);

    @Test
    @DisplayName("pending: PENDING, 시도 0회, 다음 시도는 1분 뒤")
    void pending() {
        DiscordOutbox outbox = DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, 1L, NOW);

        assertThat(outbox.getStatus()).isEqualTo(DiscordOutboxStatus.PENDING);
        assertThat(outbox.getAttemptCount()).isZero();
        assertThat(outbox.getNextAttemptAt()).isEqualTo(NOW.plusMinutes(1));
    }

    @Test
    @DisplayName("recordFailure: 1~4회 실패는 1·2·4·8분 뒤로 미루고 PENDING 유지")
    void recordFailure_backoff() {
        DiscordOutbox outbox = DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, 1L, NOW);
        long[] expectedDelays = {1, 2, 4, 8};

        for (int i = 0; i < expectedDelays.length; i++) {
            outbox.recordFailure("boom", NOW);
            assertThat(outbox.getAttemptCount()).isEqualTo(i + 1);
            assertThat(outbox.getStatus()).isEqualTo(DiscordOutboxStatus.PENDING);
            assertThat(outbox.getNextAttemptAt()).isEqualTo(NOW.plusMinutes(expectedDelays[i]));
        }
    }

    @Test
    @DisplayName("recordFailure: 5회째 실패하면 FAILED")
    void recordFailure_fifthFails() {
        DiscordOutbox outbox = DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, 1L, NOW);

        for (int i = 0; i < DiscordOutbox.MAX_ATTEMPTS; i++) {
            outbox.recordFailure("boom", NOW);
        }

        assertThat(outbox.getAttemptCount()).isEqualTo(5);
        assertThat(outbox.getStatus()).isEqualTo(DiscordOutboxStatus.FAILED);
        assertThat(outbox.isPending()).isFalse();
    }

    @Test
    @DisplayName("recordFailure: 사유는 500자로 자른다")
    void recordFailure_truncatesError() {
        DiscordOutbox outbox = DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, 1L, NOW);

        outbox.recordFailure("x".repeat(600), NOW);

        assertThat(outbox.getLastError()).hasSize(500);
    }

    @Test
    @DisplayName("markSent: SENT와 발송 시각 기록")
    void markSent() {
        DiscordOutbox outbox = DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, 1L, NOW);

        outbox.markSent(NOW.plusSeconds(3));

        assertThat(outbox.getStatus()).isEqualTo(DiscordOutboxStatus.SENT);
        assertThat(outbox.getSentAt()).isEqualTo(NOW.plusSeconds(3));
    }

    @Test
    @DisplayName("markSkipped: SKIPPED")
    void markSkipped() {
        DiscordOutbox outbox = DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, 1L, NOW);

        outbox.markSkipped();

        assertThat(outbox.getStatus()).isEqualTo(DiscordOutboxStatus.SKIPPED);
    }
}
