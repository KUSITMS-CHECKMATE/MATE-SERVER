package server.MATE.global.discord.outbox.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.extern.slf4j.Slf4j;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.discord.channel.TestAlertChannel;
import server.MATE.global.discord.outbox.entity.DiscordOutbox;
import server.MATE.global.discord.outbox.entity.DiscordOutboxType;
import server.MATE.global.discord.outbox.repository.DiscordOutboxRepository;

@Slf4j
@Service
public class DiscordOutboxService {

    private final DiscordOutboxRepository outboxRepository;
    private final TestRepository testRepository;
    private final TestAlertChannel testAlertChannel;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final String deployEnv;

    public DiscordOutboxService(
            DiscordOutboxRepository outboxRepository,
            TestRepository testRepository,
            TestAlertChannel testAlertChannel,
            PlatformTransactionManager transactionManager,
            Clock clock,
            @Value("${deploy.env:local}") String deployEnv
    ) {
        this.outboxRepository = outboxRepository;
        this.testRepository = testRepository;
        this.testAlertChannel = testAlertChannel;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
        this.deployEnv = deployEnv;
    }

    // 테스트 발행 트랜잭션 안에서 호출. 발행 롤백 시 이 행도 함께 롤백
    public void enqueueTestCreated(Long testId) {
        if ("local".equals(deployEnv)) {
            return;
        }
        outboxRepository.save(DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, testId, now()));
    }

    public void processTestCreated(Long testId) {
        outboxRepository.findByTypeAndTargetId(DiscordOutboxType.TEST_CREATED, testId)
                .ifPresent(outbox -> process(outbox.getId()));
    }

    // 읽기와 결과 기록만 짧은 트랜잭션으로 처리, HTTP 전송은 트랜잭션 밖에서 수행
    public void process(Long outboxId) {
        CreatedAlert alert = transactionTemplate.execute(status -> prepare(outboxId));
        if (alert == null) {
            return;
        }

        try {
            testAlertChannel.sendCreated(alert.testId(), alert.title(), alert.reward(), alert.createdAt());
        } catch (RuntimeException e) {
            recordFailure(outboxId, alert.testId(), e);
            return;
        }

        transactionTemplate.executeWithoutResult(status ->
                outboxRepository.findById(outboxId)
                        .filter(DiscordOutbox::isPending)
                        .ifPresent(outbox -> outbox.markSent(now())));
    }

    private CreatedAlert prepare(Long outboxId) {
        DiscordOutbox outbox = outboxRepository.findById(outboxId).orElse(null);
        if (outbox == null || !outbox.isPending()) {
            return null;
        }

        Test test = testRepository.findById(outbox.getTargetId()).orElse(null);
        if (test == null || test.getDeletedAt() != null || test.getTestStatus() != TestStatus.WAITING) {
            outbox.markSkipped();
            log.info("[DISCORD] 검수 대기가 아니어서 새 테스트 알림을 건너뜁니다. outboxId={}, testId={}",
                    outboxId, outbox.getTargetId());
            return null;
        }

        return new CreatedAlert(test.getId(), test.getTitle(), test.getReward(), test.getCreatedAt());
    }

    private void recordFailure(Long outboxId, Long testId, RuntimeException e) {
        String error = e.getClass().getSimpleName() + ": " + e.getMessage();
        transactionTemplate.executeWithoutResult(status ->
                outboxRepository.findById(outboxId)
                        .filter(DiscordOutbox::isPending)
                        .ifPresent(outbox -> {
                            outbox.recordFailure(error, now());
                            if (!outbox.isPending()) {
                                log.error("[DISCORD] 새 테스트 알림 재시도 한도 초과. outboxId={}, testId={}, attempts={}, error={}",
                                        outboxId, testId, outbox.getAttemptCount(), error);
                            }
                        }));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private record CreatedAlert(Long testId, String title, Integer reward, LocalDateTime createdAt) {
    }
}
