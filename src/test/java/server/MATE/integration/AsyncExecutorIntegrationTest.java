package server.MATE.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import server.MATE.domain.promotion.event.PromotionRewardRequestEvent;
import server.MATE.domain.promotion.service.PromotionService;
import server.MATE.global.storage.service.FileStorageService;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
class AsyncExecutorIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private PromotionService promotionService;
    @MockitoBean
    private FileStorageService fileStorageService;

    @Test
    @DisplayName("리워드 지급 리스너는 일반 비동기 풀(async-) 스레드에서 실행")
    void rewardListener_runsOnAsyncPool() {
        AtomicReference<String> threadName = new AtomicReference<>();
        doAnswer(invocation -> {
            threadName.set(Thread.currentThread().getName());
            return null;
        }).when(promotionService).grant(anyLong(), anyLong(), anyLong(), anyInt());

        publishInTransaction(new PromotionRewardRequestEvent(1L, 1L, 1L, 100));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(threadName.get()).startsWith("async-"));
    }

    private void publishInTransaction(Object event) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> eventPublisher.publishEvent(event));
    }
}
