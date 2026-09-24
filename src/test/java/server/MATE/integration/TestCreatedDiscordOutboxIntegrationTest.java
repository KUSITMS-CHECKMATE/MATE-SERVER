package server.MATE.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.event.TestCreatedEvent;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.discord.outbox.entity.DiscordOutboxStatus;
import server.MATE.global.discord.outbox.entity.DiscordOutboxType;
import server.MATE.global.discord.outbox.repository.DiscordOutboxRepository;
import server.MATE.global.discord.webhook.DiscordWebhookClient;
import server.MATE.global.storage.service.FileStorageService;

@SpringBootTest(properties = {
        "deploy.env=test",
        "discord.test-alert-webhook-url=https://discord.test/alert"
})
@ActiveProfiles("test")
class TestCreatedDiscordOutboxIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private TestRepository testRepository;
    @Autowired
    private DiscordOutboxRepository discordOutboxRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private DiscordWebhookClient discordWebhookClient;
    @MockitoBean
    private FileStorageService fileStorageService;

    private final List<Long> createdTestIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (Long testId : createdTestIds) {
            discordOutboxRepository.findByTypeAndTargetId(DiscordOutboxType.TEST_CREATED, testId)
                    .ifPresent(discordOutboxRepository::delete);
        }
        testRepository.deleteAllById(createdTestIds);
        createdTestIds.clear();
    }

    private Long publishTestInTransaction(boolean rollback) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long testId = tx.execute(status -> {
            server.MATE.domain.test.entity.Test test = testRepository.save(server.MATE.domain.test.entity.Test.builder()
                    .makerId(1L)
                    .title("outbox 통합 테스트")
                    .reward(500)
                    .testStatus(TestStatus.WAITING)
                    .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                    .build());
            eventPublisher.publishEvent(new TestCreatedEvent(test.getId(), test.getTitle(), test.getReward(), test.getCreatedAt()));
            if (rollback) {
                status.setRollbackOnly();
            }
            return test.getId();
        });
        createdTestIds.add(testId);
        return testId;
    }

    @Test
    @DisplayName("발행이 커밋되면 outbox 행이 생기고, 커밋 직후 발송되어 SENT가 된다")
    void committedPublish_enqueuesAndSends() {
        willAnswer(invocation -> null)
                .given(discordWebhookClient).sendAndWait(eq("test-alert"), any(), any());

        Long testId = publishTestInTransaction(false);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var outbox = discordOutboxRepository
                    .findByTypeAndTargetId(DiscordOutboxType.TEST_CREATED, testId).orElseThrow();
            assertThat(outbox.getStatus()).isEqualTo(DiscordOutboxStatus.SENT);
        });
    }

    @Test
    @DisplayName("발행이 롤백되면 outbox 행도 남지 않는다")
    void rolledBackPublish_leavesNoRow() {
        Long testId = publishTestInTransaction(true);

        assertThat(discordOutboxRepository.findByTypeAndTargetId(DiscordOutboxType.TEST_CREATED, testId)).isEmpty();
    }
}
