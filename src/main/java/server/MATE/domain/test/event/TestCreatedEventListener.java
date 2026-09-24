package server.MATE.domain.test.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import server.MATE.global.discord.outbox.service.DiscordOutboxService;

@Component
@RequiredArgsConstructor
public class TestCreatedEventListener {

    private final DiscordOutboxService discordOutboxService;

    // 발행 트랜잭션 안에서 알림 행 적재. 테스트만 생기고 알림 기록은 없는 상태 방지
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void enqueueAlert(TestCreatedEvent event) {
        discordOutboxService.enqueueTestCreated(event.testId());
    }

    // 커밋 직후 1회 즉시 발송. 실패한 행은 DiscordOutboxRetryScheduler가 재발송
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAlert(TestCreatedEvent event) {
        discordOutboxService.processTestCreated(event.testId());
    }
}
