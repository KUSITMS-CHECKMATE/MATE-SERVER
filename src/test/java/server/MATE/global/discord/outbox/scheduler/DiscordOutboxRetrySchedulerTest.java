package server.MATE.global.discord.outbox.scheduler;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import server.MATE.global.discord.outbox.entity.DiscordOutbox;
import server.MATE.global.discord.outbox.entity.DiscordOutboxStatus;
import server.MATE.global.discord.outbox.entity.DiscordOutboxType;
import server.MATE.global.discord.outbox.repository.DiscordOutboxRepository;
import server.MATE.global.discord.outbox.service.DiscordOutboxService;

@ExtendWith(MockitoExtension.class)
class DiscordOutboxRetrySchedulerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 24, 10, 0, 0);

    @Mock
    private DiscordOutboxRepository outboxRepository;
    @Mock
    private DiscordOutboxService outboxService;

    private DiscordOutbox outbox(long id) {
        DiscordOutbox outbox = DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, id, NOW.minusMinutes(5));
        ReflectionTestUtils.setField(outbox, "id", id);
        return outbox;
    }

    @Test
    @DisplayName("재시도 대상 각각을 처리하고, 한 건의 예외가 다음 건을 막지 않는다")
    void retriesEachAndContinuesOnError() {
        Clock clock = Clock.fixed(NOW.atZone(KST).toInstant(), KST);
        DiscordOutboxRetryScheduler scheduler = new DiscordOutboxRetryScheduler(outboxRepository, outboxService, clock);
        given(outboxRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                DiscordOutboxStatus.PENDING, NOW)).willReturn(List.of(outbox(1L), outbox(2L)));
        willThrow(new RuntimeException("db")).given(outboxService).process(1L);

        scheduler.retryPending();

        verify(outboxService).process(1L);
        verify(outboxService).process(2L);
    }
}
