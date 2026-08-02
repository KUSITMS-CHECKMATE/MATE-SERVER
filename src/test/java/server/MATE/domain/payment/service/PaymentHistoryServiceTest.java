package server.MATE.domain.payment.service;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import server.MATE.domain.payment.dto.response.PaymentHistoryResponse;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.storage.FileStorageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TestRepository testRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private PaymentHistoryService paymentHistoryService;

    @Test
    @DisplayName("발행된 테스트가 연결된 결제 건은 testId/testTitle/testStatus를 함께 반환한다")
    void getHistory_includesTestIdAndStatusWhenTestLinked() {
        Payment payment = Payment.builder()
                .draftId(1L)
                .testId(10L)
                .makerId(1L)
                .orderId("order-id")
                .orderNo("order-no")
                .payStatus(PayStatus.PAY_SUCCEEDED)
                .goalPpl(100)
                .reward(300)
                .amount(30000)
                .approvedAt(LocalDateTime.of(2026, 7, 30, 12, 0))
                .build();

        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("UX 설문조사")
                .testStatus(TestStatus.IN_PROGRESS)
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();
        ReflectionTestUtils.setField(test, "id", 10L);

        given(paymentRepository.findByMakerIdOrderByCreatedAtDesc(1L)).willReturn(List.of(payment));
        given(testRepository.findAllById(java.util.Set.of(10L))).willReturn(List.of(test));

        List<PaymentHistoryResponse> history = paymentHistoryService.getHistory(1L);

        assertThat(history).hasSize(1);
        PaymentHistoryResponse response = history.getFirst();
        assertThat(response.testId()).isEqualTo(10L);
        assertThat(response.testTitle()).isEqualTo("UX 설문조사");
        assertThat(response.testStatus()).isEqualTo(TestStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("아직 테스트가 발행되지 않은 결제 건은 testId/testTitle/testStatus가 null이다")
    void getHistory_returnsNullTestFieldsWhenTestNotPublishedYet() {
        Payment payment = Payment.builder()
                .draftId(2L)
                .testId(null)
                .makerId(1L)
                .orderId("order-id-2")
                .orderNo("order-no-2")
                .payStatus(PayStatus.PAY_SUCCEEDED)
                .goalPpl(100)
                .reward(300)
                .amount(30000)
                .approvedAt(LocalDateTime.of(2026, 7, 30, 12, 0))
                .build();

        given(paymentRepository.findByMakerIdOrderByCreatedAtDesc(1L)).willReturn(List.of(payment));
        given(testRepository.findAllById(java.util.Set.of())).willReturn(List.of());

        List<PaymentHistoryResponse> history = paymentHistoryService.getHistory(1L);

        assertThat(history).hasSize(1);
        PaymentHistoryResponse response = history.getFirst();
        assertThat(response.testId()).isNull();
        assertThat(response.testTitle()).isNull();
        assertThat(response.testStatus()).isNull();
    }
}
