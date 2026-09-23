package server.MATE.domain.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.payment.entity.PayMethod;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.entity.PublishStatus;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRestoreAttemptServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentRestoreAttemptService paymentRestoreAttemptService;

    @BeforeEach
    void setUp() {
        paymentRestoreAttemptService = new PaymentRestoreAttemptService(paymentRepository);
    }

    @Test
    @DisplayName("Payment를 찾을 수 없으면 PAYMENT_001 예외를 던진다")
    void throwsPayment001WhenPaymentNotFound() {
        when(paymentRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentRestoreAttemptService.registerRestoreAttempt(100L))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode())
                .isEqualTo(BaseErrorCode.PAYMENT_001);
    }

    @Test
    @DisplayName("이미 PUBLISHED 상태면 retryCount를 건드리지 않고 PUBLISHED를 반환한다")
    void returnsPublishedWithoutTouchingRetryCountWhenAlreadyPublished() {
        Payment payment = paymentOf(PublishStatus.PUBLISHED, 0);
        when(paymentRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(payment));

        PublishStatus result = paymentRestoreAttemptService.registerRestoreAttempt(100L);

        assertThat(result).isEqualTo(PublishStatus.PUBLISHED);
        assertThat(payment.getRetryCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("retryCount가 한도 이상이면 FAILED로 표시하고 FAILED를 반환한다")
    void marksFailedWhenRetryCountAtLimit() {
        Payment payment = paymentOf(PublishStatus.PUBLISH_PENDING, Payment.MAX_RESTORE_RETRY_COUNT);
        when(paymentRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(payment));

        PublishStatus result = paymentRestoreAttemptService.registerRestoreAttempt(100L);

        assertThat(result).isEqualTo(PublishStatus.FAILED);
        assertThat(payment.getPublishStatus()).isEqualTo(PublishStatus.FAILED);
    }

    @Test
    @DisplayName("retryCount가 한도 미만이면 증가시키고 PUBLISH_PENDING을 반환한다")
    void incrementsRetryCountWhenBelowLimit() {
        Payment payment = paymentOf(PublishStatus.PUBLISH_PENDING, Payment.MAX_RESTORE_RETRY_COUNT - 1);
        when(paymentRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(payment));

        PublishStatus result = paymentRestoreAttemptService.registerRestoreAttempt(100L);

        assertThat(result).isEqualTo(PublishStatus.PUBLISH_PENDING);
        assertThat(payment.getRetryCount()).isEqualTo(Payment.MAX_RESTORE_RETRY_COUNT);
    }

    private Payment paymentOf(PublishStatus publishStatus, int retryCount) {
        Payment payment = Payment.builder()
                .draftId(10L)
                .makerId(1L)
                .orderId("order-abc-123")
                .goalPpl(10)
                .reward(500)
                .amount(5500)
                .payMethod(PayMethod.IN_APP_PURCHASE)
                .payStatus(PayStatus.PAY_SUCCEEDED)
                .publishStatus(publishStatus)
                .approvedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(payment, "id", 100L);
        ReflectionTestUtils.setField(payment, "retryCount", retryCount);
        return payment;
    }
}
