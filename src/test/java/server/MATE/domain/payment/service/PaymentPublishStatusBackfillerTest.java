package server.MATE.domain.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.entity.PublishStatus;
import server.MATE.domain.payment.repository.PaymentRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentPublishStatusBackfillerTest {

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentPublishStatusBackfiller backfiller;

    @BeforeEach
    void setUp() {
        backfiller = new PaymentPublishStatusBackfiller(paymentRepository);
    }

    @Test
    @DisplayName("두 백필 쿼리가 모두 실행된다")
    void runsBothBackfillQueries() {
        when(paymentRepository.backfillPublishedStatus(any())).thenReturn(0);
        when(paymentRepository.backfillFailedStatus(any(), anyInt())).thenReturn(0);

        backfiller.run(null);

        verify(paymentRepository).backfillPublishedStatus(PublishStatus.PUBLISHED);
        verify(paymentRepository).backfillFailedStatus(PublishStatus.FAILED, Payment.MAX_RESTORE_RETRY_COUNT);
    }

    @Test
    @DisplayName("백필 도중 예외가 발생해도 밖으로 전파되지 않는다(앱 기동을 막지 않음)")
    void doesNotPropagateExceptionOnFailure() {
        when(paymentRepository.backfillPublishedStatus(any()))
                .thenThrow(new RuntimeException("lock wait timeout"));

        backfiller.run(null);
    }
}
