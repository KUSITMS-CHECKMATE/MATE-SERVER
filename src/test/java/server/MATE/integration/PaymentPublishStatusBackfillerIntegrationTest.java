package server.MATE.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.entity.PublishStatus;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.payment.service.PaymentPublishStatusBackfiller;
import server.MATE.global.storage.service.FileStorageService;

// toss.api.enabled 프로퍼티 없이도(운영 조건부 빈 등록과 무관하게) 백필러를 실제 Spring 빈으로
// 등록해, ApplicationRunner#run()이 앰비언트 트랜잭션 없이 호출되는 기동 시점 상황을 그대로 재현한다.
@SpringBootTest
@ActiveProfiles("test")
@Import(PaymentPublishStatusBackfillerIntegrationTest.BackfillerTestConfig.class)
class PaymentPublishStatusBackfillerIntegrationTest {

    @TestConfiguration
    static class BackfillerTestConfig {
        @Bean
        PaymentPublishStatusBackfiller paymentPublishStatusBackfiller(PaymentRepository paymentRepository) {
            return new PaymentPublishStatusBackfiller(paymentRepository);
        }
    }

    @Autowired
    private PaymentPublishStatusBackfiller paymentPublishStatusBackfiller;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("testId가 채워진 PUBLISH_PENDING row는 실제 DB에서 PUBLISHED로 백필된다")
    void backfillsPublishedStatusInDatabase() {
        Payment payment = paymentRepository.save(Payment.builder()
                .draftId(1L)
                .testId(100L)
                .makerId(1L)
                .orderId("order-1")
                .orderNo("order-no-1")
                .payStatus(PayStatus.PAY_SUCCEEDED)
                .publishStatus(PublishStatus.PUBLISH_PENDING)
                .goalPpl(10)
                .reward(1000)
                .amount(1000)
                .build());

        paymentPublishStatusBackfiller.run(null);

        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(reloaded.getPublishStatus()).isEqualTo(PublishStatus.PUBLISHED);
    }

    @Test
    @DisplayName("testId 없이 retryCount가 최대치인 PUBLISH_PENDING row는 실제 DB에서 FAILED로 백필된다")
    void backfillsFailedStatusInDatabase() {
        Payment payment = Payment.builder()
                .draftId(2L)
                .makerId(1L)
                .orderId("order-2")
                .orderNo("order-no-2")
                .payStatus(PayStatus.PAY_FAILED)
                .publishStatus(PublishStatus.PUBLISH_PENDING)
                .goalPpl(10)
                .reward(1000)
                .amount(1000)
                .build();
        for (int i = 0; i < Payment.MAX_RESTORE_RETRY_COUNT; i++) {
            payment.incrementRetryCount();
        }
        payment = paymentRepository.save(payment);

        paymentPublishStatusBackfiller.run(null);

        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(reloaded.getPublishStatus()).isEqualTo(PublishStatus.FAILED);
    }
}
